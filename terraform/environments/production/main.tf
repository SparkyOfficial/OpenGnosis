terraform {
  required_version = ">= 1.5"
  
  # Backend configuration - uncomment after running backend setup
  # backend "s3" {
  #   bucket         = "opengnosis-terraform-state-production"
  #   key            = "production/terraform.tfstate"
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
      Environment = "production"
      Project     = "OpenGnosis"
      ManagedBy   = "Terraform"
    }
  }
}

locals {
  environment = "production"
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
  vpc_cidr           = "10.2.0.0/16"
  availability_zones = ["${var.region}a", "${var.region}b", "${var.region}c"]
  
  public_subnet_cidrs  = ["10.2.1.0/24", "10.2.2.0/24", "10.2.3.0/24"]
  private_subnet_cidrs = ["10.2.11.0/24", "10.2.12.0/24", "10.2.13.0/24"]
  
  enable_nat_gateway   = true
  single_nat_gateway   = false  # Multi-AZ NAT for production
  
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
  
  node_count         = 6  # Larger for production
  node_instance_type = "r6g.xlarge"
  
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
  
  instance_class      = "db.r6g.2xlarge"
  allocated_storage   = 500
  database_name       = "opengnosis"
  master_username     = "postgres"
  master_password     = var.db_password
  
  vpc_id                 = module.networking.vpc_id
  subnet_ids             = module.networking.private_subnet_ids
  allowed_cidr_blocks    = [module.networking.vpc_cidr]
  
  backup_retention_days = 30
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
  
  node_type       = "cache.r6g.xlarge"
  num_cache_nodes = 6  # 3 shards with replication
  
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
  
  instance_type  = "r6g.2xlarge.search"
  instance_count = 6  # 3 AZs with 2 nodes each
  volume_size    = 500
  
  vpc_id              = module.networking.vpc_id
  subnet_ids          = module.networking.private_subnet_ids
  allowed_cidr_blocks = [module.networking.vpc_cidr]
  
  dedicated_master_enabled = true
  dedicated_master_type    = "r6g.xlarge.search"
  dedicated_master_count   = 3
  
  tags = local.common_tags
}

# Monitoring
module "monitoring" {
  source = "../../modules/monitoring"
  
  environment         = local.environment
  cloud_provider      = "aws"
  region              = var.region
  log_retention_days  = 90
  backup_retention_days = 30
  
  backup_bucket_name = "${local.project}-${local.environment}-backups"
  enable_prometheus  = true
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  tags = local.common_tags
}
