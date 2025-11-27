output "instance_id" {
  description = "PostgreSQL instance ID"
  value = var.cloud_provider == "aws" ? aws_db_instance.main[0].id : (
    var.cloud_provider == "gcp" ? google_sql_database_instance.main[0].id : (
      var.cloud_provider == "azure" ? azurerm_postgresql_flexible_server.main[0].id : null
    )
  )
}

output "endpoint" {
  description = "PostgreSQL connection endpoint"
  value = var.cloud_provider == "aws" ? aws_db_instance.main[0].endpoint : (
    var.cloud_provider == "gcp" ? google_sql_database_instance.main[0].connection_name : (
      var.cloud_provider == "azure" ? azurerm_postgresql_flexible_server.main[0].fqdn : null
    )
  )
}

output "host" {
  description = "PostgreSQL host"
  value = var.cloud_provider == "aws" ? aws_db_instance.main[0].address : (
    var.cloud_provider == "gcp" ? google_sql_database_instance.main[0].ip_address[0].ip_address : (
      var.cloud_provider == "azure" ? azurerm_postgresql_flexible_server.main[0].fqdn : null
    )
  )
}

output "port" {
  description = "PostgreSQL port"
  value = var.cloud_provider == "aws" ? aws_db_instance.main[0].port : (
    var.cloud_provider == "gcp" ? 5432 : (
      var.cloud_provider == "azure" ? 5432 : null
    )
  )
}

output "database_name" {
  description = "Database name"
  value       = var.database_name
}

output "master_username" {
  description = "Master username"
  value       = var.master_username
  sensitive   = true
}

output "connection_string" {
  description = "PostgreSQL connection string"
  value = var.cloud_provider == "aws" ? "postgresql://${var.master_username}:${var.master_password}@${aws_db_instance.main[0].address}:${aws_db_instance.main[0].port}/${var.database_name}" : (
    var.cloud_provider == "gcp" ? "postgresql://${var.master_username}:${var.master_password}@${google_sql_database_instance.main[0].ip_address[0].ip_address}:5432/${var.database_name}" : (
      var.cloud_provider == "azure" ? "postgresql://${var.master_username}:${var.master_password}@${azurerm_postgresql_flexible_server.main[0].fqdn}:5432/${var.database_name}" : null
    )
  )
  sensitive = true
}
