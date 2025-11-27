variable "environment" {
  description = "Environment (dev, staging, production)"
  type        = string
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

variable "log_retention_days" {
  description = "Number of days to retain logs"
  type        = number
  default     = 30
}

variable "backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 30
}

variable "backup_bucket_name" {
  description = "Name of the S3/GCS/Azure Storage bucket for backups"
  type        = string
}

variable "enable_prometheus" {
  description = "Enable managed Prometheus service"
  type        = bool
  default     = true
}

variable "vpc_id" {
  description = "VPC ID for monitoring resources"
  type        = string
  default     = ""
}

variable "subnet_ids" {
  description = "Subnet IDs for monitoring resources"
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
