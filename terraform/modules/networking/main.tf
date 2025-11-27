terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

# AWS VPC
resource "aws_vpc" "main" {
  count                = var.cloud_provider == "aws" ? 1 : 0
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = var.enable_dns_hostnames
  enable_dns_support   = var.enable_dns_support

  tags = merge(
    var.tags,
    {
      Name        = var.vpc_name
      Environment = var.environment
    }
  )
}

resource "aws_internet_gateway" "main" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  vpc_id = aws_vpc.main[0].id

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-igw"
      Environment = var.environment
    }
  )
}

resource "aws_subnet" "public" {
  count                   = var.cloud_provider == "aws" ? length(var.public_subnet_cidrs) : 0
  vpc_id                  = aws_vpc.main[0].id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index % length(var.availability_zones)]
  map_public_ip_on_launch = true

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-public-${count.index + 1}"
      Environment = var.environment
      Type        = "public"
    }
  )
}

resource "aws_subnet" "private" {
  count             = var.cloud_provider == "aws" ? length(var.private_subnet_cidrs) : 0
  vpc_id            = aws_vpc.main[0].id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index % length(var.availability_zones)]

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-private-${count.index + 1}"
      Environment = var.environment
      Type        = "private"
    }
  )
}

resource "aws_eip" "nat" {
  count  = var.cloud_provider == "aws" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.public_subnet_cidrs)) : 0
  domain = "vpc"

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-nat-eip-${count.index + 1}"
      Environment = var.environment
    }
  )

  depends_on = [aws_internet_gateway.main[0]]
}

resource "aws_nat_gateway" "main" {
  count         = var.cloud_provider == "aws" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.public_subnet_cidrs)) : 0
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-nat-${count.index + 1}"
      Environment = var.environment
    }
  )

  depends_on = [aws_internet_gateway.main[0]]
}

resource "aws_route_table" "public" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main[0].id
  }

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-public-rt"
      Environment = var.environment
    }
  )
}

resource "aws_route_table" "private" {
  count  = var.cloud_provider == "aws" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.private_subnet_cidrs)) : 0
  vpc_id = aws_vpc.main[0].id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = var.single_nat_gateway ? aws_nat_gateway.main[0].id : aws_nat_gateway.main[count.index].id
  }

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-private-rt-${count.index + 1}"
      Environment = var.environment
    }
  )
}

resource "aws_route_table_association" "public" {
  count          = var.cloud_provider == "aws" ? length(var.public_subnet_cidrs) : 0
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public[0].id
}

resource "aws_route_table_association" "private" {
  count          = var.cloud_provider == "aws" && var.enable_nat_gateway ? length(var.private_subnet_cidrs) : 0
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = var.single_nat_gateway ? aws_route_table.private[0].id : aws_route_table.private[count.index].id
}

# Network ACLs
resource "aws_network_acl" "public" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  vpc_id = aws_vpc.main[0].id
  subnet_ids = aws_subnet.public[*].id

  ingress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  egress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-public-nacl"
      Environment = var.environment
    }
  )
}

resource "aws_network_acl" "private" {
  count  = var.cloud_provider == "aws" ? 1 : 0
  vpc_id = aws_vpc.main[0].id
  subnet_ids = aws_subnet.private[*].id

  ingress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = var.vpc_cidr
    from_port  = 0
    to_port    = 0
  }

  ingress {
    protocol   = "tcp"
    rule_no    = 110
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 1024
    to_port    = 65535
  }

  egress {
    protocol   = -1
    rule_no    = 100
    action     = "allow"
    cidr_block = "0.0.0.0/0"
    from_port  = 0
    to_port    = 0
  }

  tags = merge(
    var.tags,
    {
      Name        = "${var.vpc_name}-private-nacl"
      Environment = var.environment
    }
  )
}

# GCP VPC
resource "google_compute_network" "main" {
  count                   = var.cloud_provider == "gcp" ? 1 : 0
  name                    = var.vpc_name
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "public" {
  count         = var.cloud_provider == "gcp" ? length(var.public_subnet_cidrs) : 0
  name          = "${var.vpc_name}-public-${count.index + 1}"
  ip_cidr_range = var.public_subnet_cidrs[count.index]
  region        = var.region
  network       = google_compute_network.main[0].id
}

resource "google_compute_subnetwork" "private" {
  count         = var.cloud_provider == "gcp" ? length(var.private_subnet_cidrs) : 0
  name          = "${var.vpc_name}-private-${count.index + 1}"
  ip_cidr_range = var.private_subnet_cidrs[count.index]
  region        = var.region
  network       = google_compute_network.main[0].id

  private_ip_google_access = true
}

resource "google_compute_router" "main" {
  count   = var.cloud_provider == "gcp" && var.enable_nat_gateway ? 1 : 0
  name    = "${var.vpc_name}-router"
  region  = var.region
  network = google_compute_network.main[0].id
}

resource "google_compute_router_nat" "main" {
  count                              = var.cloud_provider == "gcp" && var.enable_nat_gateway ? 1 : 0
  name                               = "${var.vpc_name}-nat"
  router                             = google_compute_router.main[0].name
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"
  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"

  dynamic "subnetwork" {
    for_each = google_compute_subnetwork.private
    content {
      name                    = subnetwork.value.id
      source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
    }
  }
}

# Azure VNet
resource "azurerm_virtual_network" "main" {
  count               = var.cloud_provider == "azure" ? 1 : 0
  name                = var.vpc_name
  address_space       = [var.vpc_cidr]
  location            = var.region
  resource_group_name = var.tags["resource_group"]

  tags = var.tags
}

resource "azurerm_subnet" "public" {
  count                = var.cloud_provider == "azure" ? length(var.public_subnet_cidrs) : 0
  name                 = "${var.vpc_name}-public-${count.index + 1}"
  resource_group_name  = var.tags["resource_group"]
  virtual_network_name = azurerm_virtual_network.main[0].name
  address_prefixes     = [var.public_subnet_cidrs[count.index]]
}

resource "azurerm_subnet" "private" {
  count                = var.cloud_provider == "azure" ? length(var.private_subnet_cidrs) : 0
  name                 = "${var.vpc_name}-private-${count.index + 1}"
  resource_group_name  = var.tags["resource_group"]
  virtual_network_name = azurerm_virtual_network.main[0].name
  address_prefixes     = [var.private_subnet_cidrs[count.index]]
}

resource "azurerm_public_ip" "nat" {
  count               = var.cloud_provider == "azure" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.public_subnet_cidrs)) : 0
  name                = "${var.vpc_name}-nat-ip-${count.index + 1}"
  location            = var.region
  resource_group_name = var.tags["resource_group"]
  allocation_method   = "Static"
  sku                 = "Standard"

  tags = var.tags
}

resource "azurerm_nat_gateway" "main" {
  count               = var.cloud_provider == "azure" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.public_subnet_cidrs)) : 0
  name                = "${var.vpc_name}-nat-${count.index + 1}"
  location            = var.region
  resource_group_name = var.tags["resource_group"]
  sku_name            = "Standard"

  tags = var.tags
}

resource "azurerm_nat_gateway_public_ip_association" "main" {
  count                = var.cloud_provider == "azure" && var.enable_nat_gateway ? (var.single_nat_gateway ? 1 : length(var.public_subnet_cidrs)) : 0
  nat_gateway_id       = azurerm_nat_gateway.main[count.index].id
  public_ip_address_id = azurerm_public_ip.nat[count.index].id
}

resource "azurerm_subnet_nat_gateway_association" "private" {
  count          = var.cloud_provider == "azure" && var.enable_nat_gateway ? length(var.private_subnet_cidrs) : 0
  subnet_id      = azurerm_subnet.private[count.index].id
  nat_gateway_id = var.single_nat_gateway ? azurerm_nat_gateway.main[0].id : azurerm_nat_gateway.main[count.index].id
}
