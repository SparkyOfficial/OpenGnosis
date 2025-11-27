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

# AWS OpenSearch (Elasticsearch)
resource "aws_security_group" "elasticsearch" {
  count       = var.cloud_provider == "aws" ? 1 : 0
  name        = "${var.cluster_name}-sg"
  description = "Security group for Elasticsearch cluster"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
  }

  ingress {
    from_port   = 9200
    to_port     = 9200
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

resource "aws_iam_service_linked_role" "elasticsearch" {
  count            = var.cloud_provider == "aws" ? 1 : 0
  aws_service_name = "opensearchservice.amazonaws.com"
}

resource "aws_opensearch_domain" "main" {
  count         = var.cloud_provider == "aws" ? 1 : 0
  domain_name   = var.cluster_name
  engine_version = "Elasticsearch_${var.elasticsearch_version}"

  cluster_config {
    instance_type            = var.instance_type
    instance_count           = var.instance_count
    dedicated_master_enabled = var.dedicated_master_enabled
    dedicated_master_type    = var.dedicated_master_enabled ? var.dedicated_master_type : null
    dedicated_master_count   = var.dedicated_master_enabled ? var.dedicated_master_count : null
    zone_awareness_enabled   = var.instance_count > 1

    dynamic "zone_awareness_config" {
      for_each = var.instance_count > 1 ? [1] : []
      content {
        availability_zone_count = min(var.instance_count, 3)
      }
    }
  }

  ebs_options {
    ebs_enabled = true
    volume_size = var.volume_size
    volume_type = "gp3"
  }

  vpc_options {
    subnet_ids         = slice(var.subnet_ids, 0, min(length(var.subnet_ids), var.instance_count > 1 ? 3 : 1))
    security_group_ids = [aws_security_group.elasticsearch[0].id]
  }

  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-2019-07"
  }

  advanced_security_options {
    enabled                        = true
    internal_user_database_enabled = true
    master_user_options {
      master_user_name     = "admin"
      master_user_password = random_password.elasticsearch_password[0].result
    }
  }

  snapshot_options {
    automated_snapshot_start_hour = 3
  }

  tags = merge(
    var.tags,
    {
      Name        = var.cluster_name
      Environment = var.environment
    }
  )

  depends_on = [aws_iam_service_linked_role.elasticsearch[0]]
}

resource "random_password" "elasticsearch_password" {
  count   = var.cloud_provider == "aws" ? 1 : 0
  length  = 16
  special = true
}

# GCP Elasticsearch (via Elastic Cloud or self-managed on GKE)
# Note: GCP doesn't have a native managed Elasticsearch service
# This would typically be deployed on GKE using Helm or operators
# For this example, we'll create a placeholder that would be used with ECK operator

# Azure doesn't have native Elasticsearch either
# Would typically use Elastic Cloud or deploy on AKS
