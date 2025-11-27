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

# AWS ElastiCache Redis
resource "aws_elasticache_subnet_group" "main" {
  count      = var.cloud_provider == "aws" ? 1 : 0
  name       = "${var.cluster_name}-subnet-group"
  subnet_ids = var.subnet_ids

  tags = merge(
    var.tags,
    {
      Name        = "${var.cluster_name}-subnet-group"
      Environment = var.environment
    }
  )
}

resource "aws_security_group" "redis" {
  count       = var.cloud_provider == "aws" ? 1 : 0
  name        = "${var.cluster_name}-sg"
  description = "Security group for Redis cluster"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 6379
    to_port     = 6379
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
      Name        = "${var.cluster_name}-sg"
      Environment = var.environment
    }
  )
}

resource "aws_elasticache_replication_group" "main" {
  count                      = var.cloud_provider == "aws" ? 1 : 0
  replication_group_id       = var.cluster_name
  description                = "Redis cluster for ${var.environment}"
  engine                     = "redis"
  engine_version             = var.redis_version
  node_type                  = var.node_type
  num_cache_clusters         = var.num_cache_nodes
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.main[0].name
  security_group_ids         = [aws_security_group.redis[0].id]
  automatic_failover_enabled = var.automatic_failover_enabled && var.num_cache_nodes > 1
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  snapshot_retention_limit   = 5
  snapshot_window            = "03:00-05:00"
  maintenance_window         = "mon:05:00-mon:07:00"

  tags = merge(
    var.tags,
    {
      Name        = var.cluster_name
      Environment = var.environment
    }
  )
}

# GCP Memorystore Redis
resource "google_redis_instance" "main" {
  count              = var.cloud_provider == "gcp" ? 1 : 0
  name               = var.cluster_name
  tier               = var.num_cache_nodes > 1 ? "STANDARD_HA" : "BASIC"
  memory_size_gb     = tonumber(split(".", var.node_type)[1])
  region             = var.region
  redis_version      = "REDIS_${replace(var.redis_version, ".", "_")}"
  authorized_network = var.vpc_id

  redis_configs = {
    maxmemory-policy = "allkeys-lru"
  }

  labels = var.tags
}

# Azure Cache for Redis
resource "azurerm_redis_cache" "main" {
  count               = var.cloud_provider == "azure" ? 1 : 0
  name                = var.cluster_name
  location            = var.region
  resource_group_name = var.vpc_id # In Azure, this would be resource group name
  capacity            = tonumber(split("C", var.node_type)[1])
  family              = "C"
  sku_name            = var.num_cache_nodes > 1 ? "Premium" : "Standard"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"

  redis_configuration {
    maxmemory_policy = "allkeys-lru"
  }

  tags = var.tags
}
