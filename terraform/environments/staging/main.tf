terraform {
  required_version = ">= 1.5"
  
  # Backend configuration - uncomment after running backend setup
  # backend "s3" {
  #   bucket         = "opengnosis-terraform-state-staging"
  #   key            = "staging/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
  
  default_tags {
    tags = {
      Environment = "staging"
      Project     = "OpenGnosis"
      ManagedBy   = "Terraform"
    }
  }
}

locals {
  environment = "staging"
  project     = "opengnosis"
  
  common_tags = {
    Environment = local.environment
    Project     = local.project
    ManagedBy   = "Terraform"
  }
}

# Networking
module "networking" {
  source = "../../modules/networking"
  
  vpc_name           = "${local.project}-${local.environment}"
  environment        = local.environment
  cloud_provider     = "aws"
  region             = var.region
  vpc_cidr           = "10.1.0.0/16"
  availability_zones = ["${var.region}a", "${var.region}b", "${var.region}c"]
  
  public_subnet_cidrs  = ["10.1.1.0/24", "10.1.2.0/24", "10.1.3.0/24"]
  private_subnet_cidrs = ["10.1.11.0/24", "10.1.12.0/24", "10.1.13.0/24"]
  
  enable_nat_gateway   = true
  single_nat_gateway   = false  # Multi-AZ NAT for staging
  
  tags = local.common_tags
}

# Kubernetes Cluster
module "kubernetes" {
  source = "../../modules/kubernetes"
  
  cluster_name       = "${local.project}-${local.environment}"
  environment        = local.environment
  cloud_provider     = "aws"
  region             = var.region
  kubernetes_version = "1.28"
  
  node_count         = 3
  node_instance_type = "t3.large"
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  tags = local.common_tags
}

# PostgreSQL Database
module "postgresql" {
  source = "../../modules/postgresql"
  
  instance_name      = "${local.project}-${local.environment}-db"
  environment        = local.environment
  cloud_provider     = "aws"
  region             = var.region
  postgres_version   = "15"
  
  instance_class      = "db.r6g.large"
  allocated_storage   = 100
  database_name       = "opengnosis"
  master_username     = "postgres"
  master_password     = var.db_password
  
  vpc_id                 = module.networking.vpc_id
  subnet_ids             = module.networking.private_subnet_ids
  allowed_cidr_blocks    = [module.networking.vpc_cidr]
  
  backup_retention_days = 14
  multi_az              = true
  
  tags = local.common_tags
}

# Redis Cache
module "redis" {
  source = "../../modules/redis"
  
  cluster_name       = "${local.project}-${local.environment}-redis"
  environment        = local.environment
  cloud_provider     = "aws"
  region             = var.region
  redis_version      = "7.0"
  
  node_type       = "cache.r6g.large"
  num_cache_nodes = 3
  
  vpc_id                      = module.networking.vpc_id
  subnet_ids                  = module.networking.private_subnet_ids
  allowed_cidr_blocks         = [module.networking.vpc_cidr]
  automatic_failover_enabled  = true
  
  tags = local.common_tags
}

# Elasticsearch
module "elasticsearch" {
  source = "../../modules/elasticsearch"
  
  cluster_name          = "${local.project}-${local.environment}-es"
  environment           = local.environment
  cloud_provider        = "aws"
  region                = var.region
  elasticsearch_version = "7.10"
  
  instance_type  = "r6g.large.search"
  instance_count = 3
  volume_size    = 100
  
  vpc_id              = module.networking.vpc_id
  subnet_ids          = module.networking.private_subnet_ids
  allowed_cidr_blocks = [module.networking.vpc_cidr]
  
  dedicated_master_enabled = true
  dedicated_master_type    = "r6g.large.search"
  dedicated_master_count   = 3
  
  tags = local.common_tags
}

# Monitoring
module "monitoring" {
  source = "../../modules/monitoring"
  
  environment         = local.environment
  cloud_provider      = "aws"
  region              = var.region
  log_retention_days  = 30
  backup_retention_days = 30
  
  backup_bucket_name = "${local.project}-${local.environment}-backups"
  enable_prometheus  = true
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  tags = local.common_tags
}
