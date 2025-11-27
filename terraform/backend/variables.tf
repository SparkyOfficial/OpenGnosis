variable "state_bucket_name" {
  description = "Name of the S3/GCS/Azure Storage bucket for Terraform state"
  type        = string
}

variable "state_lock_table_name" {
  description = "Name of the DynamoDB table for state locking (AWS only)"
  type        = string
  default     = "terraform-state-lock"
}

variable "cloud_provider" {
  description = "Cloud provider (aws, gcp, azure)"
  type        = string
  validation {
    condition     = contains(["aws", "gcp", "azure"], var.cloud_provider)
    error_message = "Cloud provider must be aws, gcp, or azure."
  }
}

variable "region" {
  description = "Cloud region"
  type        = string
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
