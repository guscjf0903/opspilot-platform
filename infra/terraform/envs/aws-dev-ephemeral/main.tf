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

  repository_names              = var.ecr_repository_names
  image_tag_mutability          = var.ecr_image_tag_mutability
  scan_on_push                  = var.ecr_scan_on_push
  expire_untagged_after_days    = var.ecr_expire_untagged_after_days
  keep_recent_images            = var.ecr_keep_recent_images
  tags                          = local.common_tags
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
