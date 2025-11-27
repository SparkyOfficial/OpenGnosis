output "state_bucket_name" {
  description = "Name of the Terraform state bucket"
  value = var.cloud_provider == "aws" ? aws_s3_bucket.terraform_state[0].id : (
    var.cloud_provider == "gcp" ? google_storage_bucket.terraform_state[0].name : (
      var.cloud_provider == "azure" ? azurerm_storage_account.terraform_state[0].name : null
    )
  )
}

output "state_bucket_arn" {
  description = "ARN/ID of the Terraform state bucket"
  value = var.cloud_provider == "aws" ? aws_s3_bucket.terraform_state[0].arn : (
    var.cloud_provider == "gcp" ? google_storage_bucket.terraform_state[0].url : (
      var.cloud_provider == "azure" ? azurerm_storage_account.terraform_state[0].id : null
    )
  )
}

output "state_lock_table_name" {
  description = "Name of the DynamoDB state lock table (AWS only)"
  value       = var.cloud_provider == "aws" ? aws_dynamodb_table.terraform_state_lock[0].name : null
}

output "state_lock_table_arn" {
  description = "ARN of the DynamoDB state lock table (AWS only)"
  value       = var.cloud_provider == "aws" ? aws_dynamodb_table.terraform_state_lock[0].arn : null
}

output "backend_config" {
  description = "Backend configuration for Terraform"
  value = var.cloud_provider == "aws" ? {
    backend = "s3"
    config = {
      bucket         = aws_s3_bucket.terraform_state[0].id
      key            = "terraform.tfstate"
      region         = var.region
      encrypt        = true
      dynamodb_table = aws_dynamodb_table.terraform_state_lock[0].name
    }
  } : (
    var.cloud_provider == "gcp" ? {
      backend = "gcs"
      config = {
        bucket = google_storage_bucket.terraform_state[0].name
        prefix = "terraform/state"
      }
    } : (
      var.cloud_provider == "azure" ? {
        backend = "azurerm"
        config = {
          storage_account_name = azurerm_storage_account.terraform_state[0].name
          container_name       = azurerm_storage_container.terraform_state[0].name
          key                  = "terraform.tfstate"
        }
      } : null
    )
  )
}
