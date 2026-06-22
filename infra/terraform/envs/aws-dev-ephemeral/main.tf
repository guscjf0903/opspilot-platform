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
}

module "ecr" {
  count  = var.enable_ecr ? 1 : 0
  source = "../../modules/ecr"

  repository_names           = var.ecr_repository_names
  image_tag_mutability       = var.ecr_image_tag_mutability
  scan_on_push               = var.ecr_scan_on_push
  expire_untagged_after_days = var.ecr_expire_untagged_after_days
  keep_recent_images         = var.ecr_keep_recent_images
  tags                       = local.common_tags
}

module "github_actions_ecr_push_role" {
  count  = var.enable_github_actions_ecr_push_role && var.enable_ecr ? 1 : 0
  source = "../../modules/github-actions-ecr-push-role"

  role_name                  = var.github_actions_ecr_push_role_name
  create_oidc_provider       = var.create_github_oidc_provider
  existing_oidc_provider_arn = var.existing_github_oidc_provider_arn
  allowed_subject_patterns   = var.github_actions_allowed_subject_patterns
  ecr_repository_arns        = var.enable_ecr ? values(module.ecr[0].repository_arns) : []
  tags                       = local.common_tags
}

module "vpc_lite" {
  count  = var.enable_vpc ? 1 : 0
  source = "../../modules/vpc-lite"

  name_prefix             = "${var.project_name}-${var.environment}"
  cidr_block              = var.vpc_cidr_block
  public_subnet_cidrs     = var.public_subnet_cidrs
  availability_zone_names = local.selected_availability_zone_names
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
  ecr_repository_arns         = var.enable_ecr ? values(module.ecr[0].repository_arns) : []
  tags                        = local.common_tags
}
