output "cluster_id" {
  description = "Redis cluster ID"
  value = var.cloud_provider == "aws" ? aws_elasticache_replication_group.main[0].id : (
    var.cloud_provider == "gcp" ? google_redis_instance.main[0].id : (
      var.cloud_provider == "azure" ? azurerm_redis_cache.main[0].id : null
    )
  )
}

output "endpoint" {
  description = "Redis connection endpoint"
  value = var.cloud_provider == "aws" ? aws_elasticache_replication_group.main[0].primary_endpoint_address : (
    var.cloud_provider == "gcp" ? google_redis_instance.main[0].host : (
      var.cloud_provider == "azure" ? azurerm_redis_cache.main[0].hostname : null
    )
  )
}

output "port" {
  description = "Redis port"
  value = var.cloud_provider == "aws" ? 6379 : (
    var.cloud_provider == "gcp" ? google_redis_instance.main[0].port : (
      var.cloud_provider == "azure" ? azurerm_redis_cache.main[0].ssl_port : null
    )
  )
}

output "connection_string" {
  description = "Redis connection string"
  value = var.cloud_provider == "aws" ? "redis://${aws_elasticache_replication_group.main[0].primary_endpoint_address}:6379" : (
    var.cloud_provider == "gcp" ? "redis://${google_redis_instance.main[0].host}:${google_redis_instance.main[0].port}" : (
      var.cloud_provider == "azure" ? "rediss://${azurerm_redis_cache.main[0].hostname}:${azurerm_redis_cache.main[0].ssl_port}" : null
    )
  )
  sensitive = true
}

output "auth_token" {
  description = "Redis authentication token"
  value = var.cloud_provider == "aws" ? null : (
    var.cloud_provider == "gcp" ? google_redis_instance.main[0].auth_string : (
      var.cloud_provider == "azure" ? azurerm_redis_cache.main[0].primary_access_key : null
    )
  )
  sensitive = true
}
