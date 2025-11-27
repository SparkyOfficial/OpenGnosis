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

# AWS S3 Bucket for Terraform State
resource "aws_s3_bucket" "terraform_state" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = var.state_bucket_name

  tags = merge(
    var.tags,
    {
      Name    = var.state_bucket_name
      Purpose = "terraform-state"
    }
  )
}

resource "aws_s3_bucket_versioning" "terraform_state" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.terraform_state[0].id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.terraform_state[0].id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "terraform_state" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.terraform_state[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "terraform_state" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  bucket = aws_s3_bucket.terraform_state[0].id

  rule {
    id     = "delete-old-versions"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }
}

# AWS DynamoDB Table for State Locking
resource "aws_dynamodb_table" "terraform_state_lock" {
  count          = var.cloud_provider == "aws" ? 1 : 0
  name           = var.state_lock_table_name
  billing_mode   = "PAY_PER_REQUEST"
  hash_key       = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = merge(
    var.tags,
    {
      Name    = var.state_lock_table_name
      Purpose = "terraform-state-lock"
    }
  )
}

# GCP Cloud Storage Bucket for Terraform State
resource "google_storage_bucket" "terraform_state" {
  count    = var.cloud_provider == "gcp" ? 1 : 0
  name     = var.state_bucket_name
  location = var.region

  versioning {
    enabled = true
  }

  uniform_bucket_level_access = true

  lifecycle_rule {
    condition {
      num_newer_versions = 10
    }
    action {
      type = "Delete"
    }
  }

  labels = var.tags
}

# Azure Storage Account for Terraform State
resource "azurerm_storage_account" "terraform_state" {
  count                    = var.cloud_provider == "azure" ? 1 : 0
  name                     = replace(var.state_bucket_name, "-", "")
  resource_group_name      = var.tags["resource_group"]
  location                 = var.region
  account_tier             = "Standard"
  account_replication_type = "GRS"
  min_tls_version          = "TLS1_2"

  blob_properties {
    versioning_enabled = true
  }

  tags = var.tags
}

resource "azurerm_storage_container" "terraform_state" {
  count                 = var.cloud_provider == "azure" ? 1 : 0
  name                  = "tfstate"
  storage_account_name  = azurerm_storage_account.terraform_state[0].name
  container_access_type = "private"
}
