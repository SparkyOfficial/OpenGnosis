## Prerequisites

1. **Terraform**: Install Terraform >= 1.5
   ```bash
   # macOS
   brew install terraform
   
   # Windows
   choco install terraform
   
   # Linux
   wget https://releases.hashicorp.com/terraform/1.5.0/terraform_1.5.0_linux_amd64.zip
   unzip terraform_1.5.0_linux_amd64.zip
   sudo mv terraform /usr/local/bin/
   ```

2. **Cloud Provider CLI**:
   - **AWS**: Install and configure AWS CLI
     ```bash
     aws configure
     ```
   - **GCP**: Install and configure gcloud CLI
     ```bash
     gcloud auth application-default login
     ```
   - **Azure**: Install and configure Azure CLI
     ```bash
     az login
     ```

3. **kubectl**: For Kubernetes cluster management
   ```bash
   # macOS
   brew install kubectl
   
   # Windows
   choco install kubernetes-cli
   ```

## Getting Started

### Step 1: Set Up Terraform State Backend

Before deploying any infrastructure, set up the remote state backend:

```bash
cd terraform/backend

# For AWS
terraform init
terraform plan -var="cloud_provider=aws" \
               -var="region=us-east-1" \
               -var="state_bucket_name=opengnosis-terraform-state" \
               -var="state_lock_table_name=terraform-state-lock"
terraform apply

# For GCP
terraform init
terraform plan -var="cloud_provider=gcp" \
               -var="region=us-central1" \
               -var="state_bucket_name=opengnosis-terraform-state"
terraform apply

# For Azure
terraform init
terraform plan -var="cloud_provider=azure" \
               -var="region=eastus" \
               -var="state_bucket_name=opengnosis-terraform-state" \
               -var='tags={"resource_group"="opengnosis-rg"}'
terraform apply
```

### Step 2: Configure Backend in Environment

After creating the state backend, uncomment the backend configuration in your environment's `main.tf`:

```hcl
# For AWS
backend "s3" {
  bucket         = "opengnosis-terraform-state"
  key            = "dev/terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "terraform-state-lock"
}

# For GCP
backend "gcs" {
  bucket = "opengnosis-terraform-state"
  prefix = "dev/terraform/state"
}

# For Azure
backend "azurerm" {
  storage_account_name = "opengnosisterraformstate"
  container_name       = "tfstate"
  key                  = "dev.terraform.tfstate"
}
```

### Step 3: Deploy Infrastructure

Deploy infrastructure for a specific environment:

```bash
cd terraform/environments/dev

# Initialize Terraform
terraform init

# Create a terraform.tfvars file with sensitive variables
cat > terraform.tfvars <<EOF
region      = "us-east-1"
db_password = "your-secure-password-here"
EOF

# Plan the deployment
terraform plan

# Apply the configuration
terraform apply
```

### Step 4: Configure kubectl

After the Kubernetes cluster is created, configure kubectl:

```bash
# For AWS EKS
aws eks update-kubeconfig --region us-east-1 --name opengnosis-dev

# For GCP GKE
gcloud container clusters get-credentials opengnosis-dev --region us-central1

# For Azure AKS
az aks get-credentials --resource-group opengnosis-rg --name opengnosis-dev
```

## Workspaces

Terraform workspaces allow you to manage multiple environments with the same configuration:

```bash
# List workspaces
terraform workspace list

# Create a new workspace
terraform workspace new staging

# Switch to a workspace
terraform workspace select staging

# Show current workspace
terraform workspace show
```

## Module Usage

### Kubernetes Module

```hcl
module "kubernetes" {
  source = "../../modules/kubernetes"
  
  cluster_name       = "my-cluster"
  environment        = "dev"
  cloud_provider     = "aws"
  region             = "us-east-1"
  kubernetes_version = "1.28"
  
  node_count         = 3
  node_instance_type = "t3.large"
  
  vpc_id     = module.networking.vpc_id
  subnet_ids = module.networking.private_subnet_ids
  
  tags = {
    Environment = "dev"
    Project     = "OpenGnosis"
  }
}
```

### PostgreSQL Module

```hcl
module "postgresql" {
  source = "../../modules/postgresql"
  
  instance_name      = "my-db"
  environment        = "dev"
  cloud_provider     = "aws"
  region             = "us-east-1"
  postgres_version   = "15"
  
  instance_class      = "db.t3.medium"
  allocated_storage   = 100
  database_name       = "myapp"
  master_username     = "postgres"
  master_password     = var.db_password
  
  vpc_id                 = module.networking.vpc_id
  subnet_ids             = module.networking.private_subnet_ids
  allowed_cidr_blocks    = [module.networking.vpc_cidr]
  
  backup_retention_days = 30
  multi_az              = true
}
```

## Security Best Practices

1. **Never commit sensitive data**: Use `.tfvars` files for secrets and add them to `.gitignore`
2. **Use remote state**: Always use remote state with encryption and locking
3. **Enable encryption**: All modules enable encryption at rest and in transit by default
4. **Least privilege**: IAM roles and security groups follow least privilege principle
5. **Multi-AZ deployment**: Production environments use multi-AZ for high availability
6. **Backup retention**: Configure appropriate backup retention for each environment

## Cost Optimization

### Development Environment
- Single NAT gateway
- Smaller instance types
- Single-node Redis
- No dedicated Elasticsearch masters
- Shorter backup retention

### Staging Environment
- Multi-AZ NAT gateways
- Medium instance types
- Multi-node Redis with failover
- Dedicated Elasticsearch masters

### Production Environment
- Multi-AZ NAT gateways
- Large instance types
- Multi-node Redis cluster
- Dedicated Elasticsearch masters
- Extended backup retention

## Troubleshooting

### State Lock Issues

If you encounter a state lock error:

```bash
# Force unlock (use with caution)
terraform force-unlock <LOCK_ID>
```

### Module Not Found

If Terraform can't find a module:

```bash
# Re-initialize to download modules
terraform init -upgrade
```

### Provider Authentication

Ensure your cloud provider credentials are properly configured:

```bash
# AWS
aws sts get-caller-identity

# GCP
gcloud auth list

# Azure
az account show
```

## Maintenance

### Updating Infrastructure

```bash
# Pull latest changes
git pull

# Review changes
terraform plan

# Apply updates
terraform apply
```

### Destroying Infrastructure

**WARNING**: This will destroy all resources. Use with extreme caution!

```bash
# Destroy specific environment
cd terraform/environments/dev
terraform destroy

# Confirm by typing 'yes'
```

## Support

For issues or questions:
1. Check the [Terraform documentation](https://www.terraform.io/docs)
2. Review module-specific README files
3. Contact the DevOps team

## License

Copyright © 2025 OpenGnosis. All rights reserved.
