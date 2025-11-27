#!/bin/bash

# Setup Terraform backend for state management
# Usage: ./setup-backend.sh <cloud_provider> <region> <environment>

set -e

CLOUD_PROVIDER=${1:-aws}
REGION=${2:-us-east-1}
ENVIRONMENT=${3:-dev}

echo "Setting up Terraform backend for ${CLOUD_PROVIDER} in ${REGION} (${ENVIRONMENT})"

cd "$(dirname "$0")/../backend"

case $CLOUD_PROVIDER in
  aws)
    BUCKET_NAME="opengnosis-terraform-state-${ENVIRONMENT}"
    LOCK_TABLE="terraform-state-lock-${ENVIRONMENT}"
    
    echo "Creating S3 bucket: ${BUCKET_NAME}"
    echo "Creating DynamoDB table: ${LOCK_TABLE}"
    
    terraform init
    terraform plan \
      -var="cloud_provider=aws" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${BUCKET_NAME}" \
      -var="state_lock_table_name=${LOCK_TABLE}" \
      -var='tags={"Environment":"'${ENVIRONMENT}'","Project":"OpenGnosis"}'
    
    terraform apply \
      -var="cloud_provider=aws" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${BUCKET_NAME}" \
      -var="state_lock_table_name=${LOCK_TABLE}" \
      -var='tags={"Environment":"'${ENVIRONMENT}'","Project":"OpenGnosis"}' \
      -auto-approve
    
    echo ""
    echo "Backend setup complete!"
    echo "Add this to your environment's main.tf:"
    echo ""
    echo "backend \"s3\" {"
    echo "  bucket         = \"${BUCKET_NAME}\""
    echo "  key            = \"${ENVIRONMENT}/terraform.tfstate\""
    echo "  region         = \"${REGION}\""
    echo "  encrypt        = true"
    echo "  dynamodb_table = \"${LOCK_TABLE}\""
    echo "}"
    ;;
    
  gcp)
    BUCKET_NAME="opengnosis-terraform-state-${ENVIRONMENT}"
    
    echo "Creating GCS bucket: ${BUCKET_NAME}"
    
    terraform init
    terraform plan \
      -var="cloud_provider=gcp" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${BUCKET_NAME}" \
      -var='tags={"environment":"'${ENVIRONMENT}'","project":"opengnosis"}'
    
    terraform apply \
      -var="cloud_provider=gcp" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${BUCKET_NAME}" \
      -var='tags={"environment":"'${ENVIRONMENT}'","project":"opengnosis"}' \
      -auto-approve
    
    echo ""
    echo "Backend setup complete!"
    echo "Add this to your environment's main.tf:"
    echo ""
    echo "backend \"gcs\" {"
    echo "  bucket = \"${BUCKET_NAME}\""
    echo "  prefix = \"${ENVIRONMENT}/terraform/state\""
    echo "}"
    ;;
    
  azure)
    STORAGE_ACCOUNT="ogterraformstate${ENVIRONMENT}"
    
    echo "Creating Azure Storage Account: ${STORAGE_ACCOUNT}"
    
    # Create resource group first
    RESOURCE_GROUP="opengnosis-terraform-${ENVIRONMENT}"
    az group create --name ${RESOURCE_GROUP} --location ${REGION}
    
    terraform init
    terraform plan \
      -var="cloud_provider=azure" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${STORAGE_ACCOUNT}" \
      -var='tags={"resource_group":"'${RESOURCE_GROUP}'","environment":"'${ENVIRONMENT}'","project":"opengnosis"}'
    
    terraform apply \
      -var="cloud_provider=azure" \
      -var="region=${REGION}" \
      -var="state_bucket_name=${STORAGE_ACCOUNT}" \
      -var='tags={"resource_group":"'${RESOURCE_GROUP}'","environment":"'${ENVIRONMENT}'","project":"opengnosis"}' \
      -auto-approve
    
    echo ""
    echo "Backend setup complete!"
    echo "Add this to your environment's main.tf:"
    echo ""
    echo "backend \"azurerm\" {"
    echo "  storage_account_name = \"${STORAGE_ACCOUNT}\""
    echo "  container_name       = \"tfstate\""
    echo "  key                  = \"${ENVIRONMENT}.terraform.tfstate\""
    echo "}"
    ;;
    
  *)
    echo "Unsupported cloud provider: ${CLOUD_PROVIDER}"
    echo "Supported providers: aws, gcp, azure"
    exit 1
    ;;
esac

echo ""
echo "Next steps:"
echo "1. Update your environment's main.tf with the backend configuration above"
echo "2. Run 'terraform init' in your environment directory"
echo "3. Deploy your infrastructure with 'terraform apply'"
