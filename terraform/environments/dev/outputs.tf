output "vpc_id" {
  description = "VPC ID"
  value       = module.networking.vpc_id
}

output "kubernetes_cluster_name" {
  description = "Kubernetes cluster name"
  value       = module.kubernetes.cluster_name
}

output "kubernetes_endpoint" {
  description = "Kubernetes cluster endpoint"
  value       = module.kubernetes.cluster_endpoint
  sensitive   = true
}

output "postgresql_endpoint" {
  description = "PostgreSQL endpoint"
  value       = module.postgresql.endpoint
}

output "redis_endpoint" {
  description = "Redis endpoint"
  value       = module.redis.endpoint
}

output "elasticsearch_endpoint" {
  description = "Elasticsearch endpoint"
  value       = module.elasticsearch.endpoint
}

output "backup_bucket" {
  description = "Backup storage bucket"
  value       = module.monitoring.backup_bucket_name
}

output "prometheus_workspace_id" {
  description = "Prometheus workspace ID"
  value       = module.monitoring.prometheus_workspace_id
}
