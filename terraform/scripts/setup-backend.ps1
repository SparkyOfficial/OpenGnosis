# Setup Terraform backend for state management
# Usage: .\setup-backend.ps1 -CloudProvider aws -Region us-east-1 -Environment dev

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("aws", "gcp", "azure")]
    [string]$CloudProvider = "aws",
    
    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1",
    
    [Parameter(Mandatory=$false)]
    [ValidateSet("dev", "staging", "production")]
    [string]$Environment = "dev"
)

$ErrorActionPreference = "Stop"

Write-Host "Setting up Terraform backend for $CloudProvider in $Region ($Environment)" -ForegroundColor Green

$BackendDir = Join-Path $PSScriptRoot "..\backend"
Set-Location $BackendDir

switch ($CloudProvider) {
    "aws" {
        $BucketName = "opengnosis-terraform-state-$Environment"
        $LockTable = "terraform-state-lock-$Environment"
        
        Write-Host "Creating S3 bucket: $BucketName" -ForegroundColor Cyan
        Write-Host "Creating DynamoDB table: $LockTable" -ForegroundColor Cyan
        
        terraform init
        terraform plan `
            -var="cloud_provider=aws" `
            -var="region=$Region" `
            -var="state_bucket_name=$BucketName" `
            -var="state_lock_table_name=$LockTable" `
            -var='tags={"Environment":"'$Environment'","Project":"OpenGnosis"}'
        
        terraform apply `
            -var="cloud_provider=aws" `
            -var="region=$Region" `
            -var="state_bucket_name=$BucketName" `
            -var="state_lock_table_name=$LockTable" `
            -var='tags={"Environment":"'$Environment'","Project":"OpenGnosis"}' `
            -auto-approve
        
        Write-Host ""
        Write-Host "Backend setup complete!" -ForegroundColor Green
        Write-Host "Add this to your environment's main.tf:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "backend `"s3`" {"
        Write-Host "  bucket         = `"$BucketName`""
        Write-Host "  key            = `"$Environment/terraform.tfstate`""
        Write-Host "  region         = `"$Region`""
        Write-Host "  encrypt        = true"
        Write-Host "  dynamodb_table = `"$LockTable`""
        Write-Host "}"
    }
    
    "gcp" {
        $BucketName = "opengnosis-terraform-state-$Environment"
        
        Write-Host "Creating GCS bucket: $BucketName" -ForegroundColor Cyan
        
        terraform init
        terraform plan `
            -var="cloud_provider=gcp" `
            -var="region=$Region" `
            -var="state_bucket_name=$BucketName" `
            -var='tags={"environment":"'$Environment'","project":"opengnosis"}'
        
        terraform apply `
            -var="cloud_provider=gcp" `
            -var="region=$Region" `
            -var="state_bucket_name=$BucketName" `
            -var='tags={"environment":"'$Environment'","project":"opengnosis"}' `
            -auto-approve
        
        Write-Host ""
        Write-Host "Backend setup complete!" -ForegroundColor Green
        Write-Host "Add this to your environment's main.tf:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "backend `"gcs`" {"
        Write-Host "  bucket = `"$BucketName`""
        Write-Host "  prefix = `"$Environment/terraform/state`""
        Write-Host "}"
    }
    
    "azure" {
        $StorageAccount = "ogterraformstate$Environment"
        $ResourceGroup = "opengnosis-terraform-$Environment"
        
        Write-Host "Creating Azure Storage Account: $StorageAccount" -ForegroundColor Cyan
        
        # Create resource group first
        az group create --name $ResourceGroup --location $Region
        
        terraform init
        terraform plan `
            -var="cloud_provider=azure" `
            -var="region=$Region" `
            -var="state_bucket_name=$StorageAccount" `
            -var='tags={"resource_group":"'$ResourceGroup'","environment":"'$Environment'","project":"opengnosis"}'
        
        terraform apply `
            -var="cloud_provider=azure" `
            -var="region=$Region" `
            -var="state_bucket_name=$StorageAccount" `
            -var='tags={"resource_group":"'$ResourceGroup'","environment":"'$Environment'","project":"opengnosis"}' `
            -auto-approve
        
        Write-Host ""
        Write-Host "Backend setup complete!" -ForegroundColor Green
        Write-Host "Add this to your environment's main.tf:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "backend `"azurerm`" {"
        Write-Host "  storage_account_name = `"$StorageAccount`""
        Write-Host "  container_name       = `"tfstate`""
        Write-Host "  key                  = `"$Environment.terraform.tfstate`""
        Write-Host "}"
    }
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Update your environment's main.tf with the backend configuration above"
Write-Host "2. Run 'terraform init' in your environment directory"
Write-Host "3. Deploy your infrastructure with 'terraform apply'"
