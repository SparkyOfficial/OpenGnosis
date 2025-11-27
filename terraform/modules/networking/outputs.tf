output "vpc_id" {
  description = "VPC ID"
  value = var.cloud_provider == "aws" ? aws_vpc.main[0].id : (
    var.cloud_provider == "gcp" ? google_compute_network.main[0].id : (
      var.cloud_provider == "azure" ? azurerm_virtual_network.main[0].id : null
    )
  )
}

output "vpc_cidr" {
  description = "VPC CIDR block"
  value       = var.vpc_cidr
}

output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value = var.cloud_provider == "aws" ? aws_subnet.public[*].id : (
    var.cloud_provider == "gcp" ? google_compute_subnetwork.public[*].id : (
      var.cloud_provider == "azure" ? azurerm_subnet.public[*].id : []
    )
  )
}

output "private_subnet_ids" {
  description = "List of private subnet IDs"
  value = var.cloud_provider == "aws" ? aws_subnet.private[*].id : (
    var.cloud_provider == "gcp" ? google_compute_subnetwork.private[*].id : (
      var.cloud_provider == "azure" ? azurerm_subnet.private[*].id : []
    )
  )
}

output "nat_gateway_ids" {
  description = "List of NAT gateway IDs"
  value = var.cloud_provider == "aws" && var.enable_nat_gateway ? aws_nat_gateway.main[*].id : (
    var.cloud_provider == "gcp" && var.enable_nat_gateway ? [google_compute_router_nat.main[0].id] : (
      var.cloud_provider == "azure" && var.enable_nat_gateway ? azurerm_nat_gateway.main[*].id : []
    )
  )
}

output "internet_gateway_id" {
  description = "Internet gateway ID (AWS only)"
  value       = var.cloud_provider == "aws" ? aws_internet_gateway.main[0].id : null
}
