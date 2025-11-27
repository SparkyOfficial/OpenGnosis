terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

# AWS CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "application" {
  count             = var.cloud_provider == "aws" ? 1 : 0
  name              = "/aws/opengnosis/${var.environment}"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "opengnosis-${var.environment}-logs"
      Environment = var.environment
    }
  )
}

resource "aws_cloudwatch_log_group" "kubernetes" {
  count             = var.cloud_provider == "aws" ? 1 : 0
  name              = "/aws/eks/opengnosis-${var.environment}/cluster"
  retention_in_days = var.log_retention_days

  tags = merge(
    var.tags,
    {
      Name        = "opengnosis-${var.environment}-k8s-logs"
      Environment = var.environment
    }
  )
}

# AWS Managed Prometheus
resource "aws_prometheus_workspace" "main" {
  count = var.cloud_provider == "aws" && var.enable_prometheus ? 1 : 0
  alias = "opengnosis-${var.environment}"

  tags = merge(
    var.tags,
    {
      Name        = "opengnosis-${var.environment}-prometheus"
      Environment = var.environment
    }
  )
}

# AWS S3 Bucket for Backups
resource "aws_s3_bucket" "backups" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = var.backup_bucket_name

  tags = merge(
    var.tags,
    {
      Name        = var.backup_bucket_name
      Environment = var.environment
      Purpose     = "database-backups"
    }
  )
}

resource "aws_s3_bucket_versioning" "backups" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id

  rule {
    id     = "delete-old-backups"
    status = "Enabled"

    expiration {
      days = var.backup_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = 7
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "backups" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.backups[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# GCP Cloud Logging
resource "google_logging_project_sink" "main" {
  count       = var.cloud_provider == "gcp" ? 1 : 0
  name        = "opengnosis-${var.environment}-sink"
  destination = "storage.googleapis.com/${google_storage_bucket.logs[0].name}"
  filter      = "resource.type = k8s_cluster"

  unique_writer_identity = true
}

resource "google_storage_bucket" "logs" {
  count    = var.cloud_provider == "gcp" ? 1 : 0
  name     = "${var.backup_bucket_name}-logs"
  location = var.region

  lifecycle_rule {
    condition {
      age = var.log_retention_days
    }
    action {
      type = "Delete"
    }
  }

  labels = var.tags
}

# GCP Cloud Monitoring (Managed Prometheus)
resource "google_monitoring_dashboard" "main" {
  count          = var.cloud_provider == "gcp" && var.enable_prometheus ? 1 : 0
  dashboard_json = jsonencode({
    displayName = "OpenGnosis ${var.environment}"
    mosaicLayout = {
      columns = 12
      tiles   = []
    }
  })
}

# GCP Cloud Storage for Backups
resource "google_storage_bucket" "backups" {
  count    = var.cloud_provider == "gcp" ? 1 : 0
  name     = var.backup_bucket_name
  location = var.region

  versioning {
    enabled = true
  }

  lifecycle_rule {
    condition {
      age = var.backup_retention_days
    }
    action {
      type = "Delete"
    }
  }

  lifecycle_rule {
    condition {
      num_newer_versions = 3
    }
    action {
      type = "Delete"
    }
  }

  labels = var.tags
}

# Azure Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "main" {
  count               = var.cloud_provider == "azure" ? 1 : 0
  name                = "opengnosis-${var.environment}-logs"
  location            = var.region
  resource_group_name = var.tags["resource_group"]
  sku                 = "PerGB2018"
  retention_in_days   = var.log_retention_days

  tags = var.tags
}

# Azure Monitor Workspace (Managed Prometheus)
resource "azurerm_monitor_workspace" "main" {
  count               = var.cloud_provider == "azure" && var.enable_prometheus ? 1 : 0
  name                = "opengnosis-${var.environment}-prometheus"
  location            = var.region
  resource_group_name = var.tags["resource_group"]

  tags = var.tags
}

# Azure Storage Account for Backups
resource "azurerm_storage_account" "backups" {
  count                    = var.cloud_provider == "azure" ? 1 : 0
  name                     = replace(var.backup_bucket_name, "-", "")
  resource_group_name      = var.tags["resource_group"]
  location                 = var.region
  account_tier             = "Standard"
  account_replication_type = "GRS"
  min_tls_version          = "TLS1_2"

  blob_properties {
    versioning_enabled = true

    delete_retention_policy {
      days = var.backup_retention_days
    }
  }

  tags = var.tags
}

resource "azurerm_storage_container" "backups" {
  count                 = var.cloud_provider == "azure" ? 1 : 0
  name                  = "database-backups"
  storage_account_name  = azurerm_storage_account.backups[0].name
  container_access_type = "private"
}

resource "azurerm_storage_management_policy" "backups" {
  count              = var.cloud_provider == "azure" ? 1 : 0
  storage_account_id = azurerm_storage_account.backups[0].id

  rule {
    name    = "delete-old-backups"
    enabled = true
    filters {
      blob_types = ["blockBlob"]
    }
    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.backup_retention_days
      }
      version {
        delete_after_days_since_creation = 7
      }
    }
  }
}
