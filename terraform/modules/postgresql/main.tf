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

# AWS RDS PostgreSQL
resource "aws_db_subnet_group" "main" {
  count      = var.cloud_provider == "aws" ? 1 : 0
  name       = "${var.instance_name}-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "${var.instance_name}-subnet-group"
      Environment = var.environment
    }
  )
}

resource "aws_security_group" "rds" {
  count       = var.cloud_provider == "aws" ? 1 : 0
  name        = "${var.instance_name}-sg"
  description = "Security group for RDS PostgreSQL"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    var.tags,
    {
      Name        = "${var.instance_name}-sg"
      Environment = var.environment
    }
  )
}

resource "aws_db_instance" "main" {
  count                  = var.cloud_provider == "aws" ? 1 : 0
  identifier             = var.instance_name
  engine                 = "postgres"
  engine_version         = var.postgres_version
  instance_class         = var.instance_class
  allocated_storage      = var.allocated_storage
  storage_type           = "gp3"
  storage_encrypted      = true
  db_name                = var.database_name
  username               = var.master_username
  password               = var.master_password
  db_subnet_group_name   = aws_db_subnet_group.main[0].name
  vpc_security_group_ids = [aws_security_group.rds[0].id]
  multi_az               = var.multi_az
  backup_retention_period = var.backup_retention_days
  backup_window          = "03:00-04:00"
  maintenance_window     = "mon:04:00-mon:05:00"
  skip_final_snapshot    = var.environment != "production"
  final_snapshot_identifier = var.environment == "production" ? "${var.instance_name}-final-snapshot" : null

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = merge(
    var.tags,
    {
      Name        = var.instance_name
      Environment = var.environment
    }
  )
}

# GCP Cloud SQL PostgreSQL
resource "google_sql_database_instance" "main" {
  count            = var.cloud_provider == "gcp" ? 1 : 0
  name             = var.instance_name
  database_version = "POSTGRES_${var.postgres_version}"
  region           = var.region

  settings {
    tier              = var.instance_class
    availability_type = var.multi_az ? "REGIONAL" : "ZONAL"
    disk_size         = var.allocated_storage
    disk_type         = "PD_SSD"

    backup_configuration {
      enabled                        = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
      transaction_log_retention_days = var.backup_retention_days
    }

    ip_configuration {
      ipv4_enabled    = true
      private_network = var.vpc_id
      dynamic "authorized_networks" {
        for_each = var.allowed_cidr_blocks
        content {
          value = authorized_networks.value
        }
      }
    }

    database_flags {
      name  = "max_connections"
      value = "200"
    }
  }

  deletion_protection = var.environment == "production"
}

resource "google_sql_database" "main" {
  count    = var.cloud_provider == "gcp" ? 1 : 0
  name     = var.database_name
  instance = google_sql_database_instance.main[0].name
}

resource "google_sql_user" "main" {
  count    = var.cloud_provider == "gcp" ? 1 : 0
  name     = var.master_username
  instance = google_sql_database_instance.main[0].name
  password = var.master_password
}

# Azure Database for PostgreSQL
resource "azurerm_postgresql_flexible_server" "main" {
  count               = var.cloud_provider == "azure" ? 1 : 0
  name                = var.instance_name
  resource_group_name = var.vpc_id # In Azure, this would be resource group name
  location            = var.region
  version             = var.postgres_version
  sku_name            = var.instance_class
  storage_mb          = var.allocated_storage * 1024

  administrator_login    = var.master_username
  administrator_password = var.master_password

  backup_retention_days        = var.backup_retention_days
  geo_redundant_backup_enabled = var.multi_az

  high_availability {
    mode = var.multi_az ? "ZoneRedundant" : "Disabled"
  }

  tags = var.tags
}

resource "azurerm_postgresql_flexible_server_database" "main" {
  count     = var.cloud_provider == "azure" ? 1 : 0
  name      = var.database_name
  server_id = azurerm_postgresql_flexible_server.main[0].id
  charset   = "UTF8"
  collation = "en_US.utf8"
}

resource "azurerm_postgresql_flexible_server_firewall_rule" "main" {
  count            = var.cloud_provider == "azure" ? length(var.allowed_cidr_blocks) : 0
  name             = "allow-cidr-${count.index}"
  server_id        = azurerm_postgresql_flexible_server.main[0].id
  start_ip_address = split("/", var.allowed_cidr_blocks[count.index])[0]
  end_ip_address   = split("/", var.allowed_cidr_blocks[count.index])[0]
}
