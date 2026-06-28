data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  common_tags = merge(var.extra_tags, {
    Project          = var.project_name
    Environment      = var.environment
    ManagedBy        = "Terraform"
    CostMode         = "portfolio-low-cost"
    ExpireAfterHours = tostring(var.default_ttl_hours)
  })

  selected_availability_zone_names = slice(
    data.aws_availability_zones.available.names,
    0,
    length(var.public_subnet_cidrs)
  )

  eks_cluster_name = coalesce(var.eks_cluster_name, "${var.project_name}-${var.environment}-eks")
}

module "vpc_lite" {
  count  = var.enable_vpc ? 1 : 0
  source = "../../modules/vpc-lite"

  name_prefix             = "${var.project_name}-${var.environment}"
  cidr_block              = var.vpc_cidr_block
  public_subnet_cidrs     = var.public_subnet_cidrs
  availability_zone_names = local.selected_availability_zone_names
  map_public_ip_on_launch = var.enable_eks
  tags                    = local.common_tags
}

module "k3s_lab" {
  count  = var.enable_k3s_lab && var.enable_vpc ? 1 : 0
  source = "../../modules/ec2-k3s-lab"

  name_prefix                 = "${var.project_name}-${var.environment}"
  vpc_id                      = module.vpc_lite[0].vpc_id
  subnet_id                   = module.vpc_lite[0].public_subnet_ids[var.k3s_lab_subnet_index]
  instance_type               = var.k3s_lab_instance_type
  root_volume_size_gb         = var.k3s_lab_root_volume_size_gb
  key_name                    = var.k3s_lab_key_name
  allowed_ssh_cidr_blocks     = var.k3s_lab_allowed_ssh_cidr_blocks
  allowed_k3s_api_cidr_blocks = var.k3s_lab_allowed_k3s_api_cidr_blocks
  allowed_http_cidr_blocks    = var.k3s_lab_allowed_http_cidr_blocks
  k3s_channel                 = var.k3s_lab_k3s_channel
  ecr_repository_arns         = values(var.foundation_ecr_repository_arns)
  tags                        = local.common_tags
}

module "eks_ephemeral" {
  count  = var.enable_eks && var.enable_vpc ? 1 : 0
  source = "../../modules/eks-ephemeral"

  cluster_name                 = local.eks_cluster_name
  kubernetes_version           = var.eks_kubernetes_version
  public_subnet_ids            = module.vpc_lite[0].public_subnet_ids
  endpoint_public_access       = var.eks_endpoint_public_access
  endpoint_private_access      = var.eks_endpoint_private_access
  endpoint_public_access_cidrs = var.eks_endpoint_public_access_cidrs
  node_group_instance_types    = var.eks_node_group_instance_types
  node_group_ami_type          = var.eks_node_group_ami_type
  node_group_capacity_type     = var.eks_node_group_capacity_type
  node_group_desired_size      = var.eks_node_group_desired_size
  node_group_min_size          = var.eks_node_group_min_size
  node_group_max_size          = var.eks_node_group_max_size
  node_group_disk_size_gb      = var.eks_node_group_disk_size_gb
  tags                         = local.common_tags
}

resource "terraform_data" "runtime_mode_guard" {
  input = {
    enable_eks     = var.enable_eks
    enable_k3s_lab = var.enable_k3s_lab
    enable_vpc     = var.enable_vpc
  }

  lifecycle {
    precondition {
      condition     = !(var.enable_eks && var.enable_k3s_lab)
      error_message = "Enable only one Kubernetes runtime at a time: enable_eks=true for Mode B or enable_k3s_lab=true for Mode B Lite."
    }

    precondition {
      condition     = !var.enable_eks || var.enable_vpc
      error_message = "enable_eks=true requires enable_vpc=true because EKS needs subnets."
    }
  }
}

resource "terraform_data" "k3s_lab_foundation_ecr_guard" {
  count = var.enable_k3s_lab ? 1 : 0
  input = var.foundation_ecr_repository_arns

  lifecycle {
    precondition {
      condition     = length(var.foundation_ecr_repository_arns) > 0
      error_message = "enable_k3s_lab=true requires foundation_ecr_repository_arns from aws-dev-foundation output so the EC2 role can pull ECR images."
    }

    precondition {
      condition     = var.enable_vpc
      error_message = "enable_k3s_lab=true requires enable_vpc=true because the k3s lab needs a subnet."
    }
  }
}
