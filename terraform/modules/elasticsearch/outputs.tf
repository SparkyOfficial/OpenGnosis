output "cluster_id" {
  description = "Elasticsearch cluster ID"
  value       = var.cloud_provider == "aws" ? aws_opensearch_domain.main[0].domain_id : null
}

output "endpoint" {
  description = "Elasticsearch endpoint"
  value       = var.cloud_provider == "aws" ? aws_opensearch_domain.main[0].endpoint : null
}

output "kibana_endpoint" {
  description = "Kibana endpoint"
  value       = var.cloud_provider == "aws" ? aws_opensearch_domain.main[0].kibana_endpoint : null
}

output "domain_name" {
  description = "Elasticsearch domain name"
  value       = var.cloud_provider == "aws" ? aws_opensearch_domain.main[0].domain_name : null
}

output "master_user_password" {
  description = "Master user password"
  value       = var.cloud_provider == "aws" ? random_password.elasticsearch_password[0].result : null
  sensitive   = true
}

output "connection_url" {
  description = "Elasticsearch connection URL"
  value       = var.cloud_provider == "aws" ? "https://${aws_opensearch_domain.main[0].endpoint}" : null
}
