output "cluster_id" {
  description = "Kubernetes cluster ID"
  value = var.cloud_provider == "aws" ? aws_eks_cluster.main[0].id : (
    var.cloud_provider == "gcp" ? google_container_cluster.main[0].id : (
      var.cloud_provider == "azure" ? azurerm_kubernetes_cluster.main[0].id : null
    )
  )
}

output "cluster_name" {
  description = "Kubernetes cluster name"
  value = var.cloud_provider == "aws" ? aws_eks_cluster.main[0].name : (
    var.cloud_provider == "gcp" ? google_container_cluster.main[0].name : (
      var.cloud_provider == "azure" ? azurerm_kubernetes_cluster.main[0].name : null
    )
  )
}

output "cluster_endpoint" {
  description = "Kubernetes cluster endpoint"
  value = var.cloud_provider == "aws" ? aws_eks_cluster.main[0].endpoint : (
    var.cloud_provider == "gcp" ? google_container_cluster.main[0].endpoint : (
      var.cloud_provider == "azure" ? azurerm_kubernetes_cluster.main[0].kube_config[0].host : null
    )
  )
  sensitive = true
}

output "cluster_ca_certificate" {
  description = "Kubernetes cluster CA certificate"
  value = var.cloud_provider == "aws" ? base64decode(aws_eks_cluster.main[0].certificate_authority[0].data) : (
    var.cloud_provider == "gcp" ? base64decode(google_container_cluster.main[0].master_auth[0].cluster_ca_certificate) : (
      var.cloud_provider == "azure" ? base64decode(azurerm_kubernetes_cluster.main[0].kube_config[0].cluster_ca_certificate) : null
    )
  )
  sensitive = true
}

output "kubeconfig" {
  description = "Kubeconfig for cluster access"
  value = var.cloud_provider == "aws" ? {
    cluster_name = aws_eks_cluster.main[0].name
    endpoint     = aws_eks_cluster.main[0].endpoint
    ca_data      = aws_eks_cluster.main[0].certificate_authority[0].data
  } : (
    var.cloud_provider == "gcp" ? {
      cluster_name = google_container_cluster.main[0].name
      endpoint     = google_container_cluster.main[0].endpoint
      ca_data      = google_container_cluster.main[0].master_auth[0].cluster_ca_certificate
    } : (
      var.cloud_provider == "azure" ? {
        cluster_name = azurerm_kubernetes_cluster.main[0].name
        endpoint     = azurerm_kubernetes_cluster.main[0].kube_config[0].host
        ca_data      = azurerm_kubernetes_cluster.main[0].kube_config[0].cluster_ca_certificate
      } : null
    )
  )
  sensitive = true
}
