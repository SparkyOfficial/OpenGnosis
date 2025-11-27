output "log_group_name" {
  description = "Name of the log group"
  value = var.cloud_provider == "aws" ? aws_cloudwatch_log_group.application[0].name : (
    var.cloud_provider == "gcp" ? google_logging_project_sink.main[0].name : (
      var.cloud_provider == "azure" ? azurerm_log_analytics_workspace.main[0].name : null
    )
  )
}

output "prometheus_workspace_id" {
  description = "Prometheus workspace ID"
  value = var.cloud_provider == "aws" && var.enable_prometheus ? aws_prometheus_workspace.main[0].id : (
    var.cloud_provider == "gcp" && var.enable_prometheus ? google_monitoring_dashboard.main[0].id : (
      var.cloud_provider == "azure" && var.enable_prometheus ? azurerm_monitor_workspace.main[0].id : null
    )
  )
}

output "prometheus_endpoint" {
  description = "Prometheus endpoint URL"
  value = var.cloud_provider == "aws" && var.enable_prometheus ? aws_prometheus_workspace.main[0].prometheus_endpoint : (
    var.cloud_provider == "azure" && var.enable_prometheus ? azurerm_monitor_workspace.main[0].query_endpoint : null
  )
}

output "backup_bucket_name" {
  description = "Name of the backup storage bucket"
  value = var.cloud_provider == "aws" ? aws_s3_bucket.backups[0].id : (
    var.cloud_provider == "gcp" ? google_storage_bucket.backups[0].name : (
      var.cloud_provider == "azure" ? azurerm_storage_account.backups[0].name : null
    )
  )
}

output "backup_bucket_arn" {
  description = "ARN/ID of the backup storage bucket"
  value = var.cloud_provider == "aws" ? aws_s3_bucket.backups[0].arn : (
    var.cloud_provider == "gcp" ? google_storage_bucket.backups[0].url : (
      var.cloud_provider == "azure" ? azurerm_storage_account.backups[0].id : null
    )
  )
}

output "log_analytics_workspace_id" {
  description = "Log Analytics Workspace ID (Azure only)"
  value       = var.cloud_provider == "azure" ? azurerm_log_analytics_workspace.main[0].workspace_id : null
}
