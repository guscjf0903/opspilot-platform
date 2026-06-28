locals {
  common_tags = merge(var.extra_tags, {
    Project          = var.project_name
    Environment      = var.environment
    ManagedBy        = "Terraform"
    CostMode         = "portfolio-low-cost"
    ExpireAfterHours = tostring(var.default_ttl_hours)
  })
}

module "ecr" {
  source = "../../modules/ecr"

  repository_names           = var.ecr_repository_names
  image_tag_mutability       = var.ecr_image_tag_mutability
  scan_on_push               = var.ecr_scan_on_push
  expire_untagged_after_days = var.ecr_expire_untagged_after_days
  keep_recent_images         = var.ecr_keep_recent_images
  tags                       = local.common_tags
}

module "github_actions_ecr_push_role" {
  source = "../../modules/github-actions-ecr-push-role"

  role_name                  = var.github_actions_ecr_push_role_name
  create_oidc_provider       = var.create_github_oidc_provider
  existing_oidc_provider_arn = var.existing_github_oidc_provider_arn
  allowed_subject_patterns   = var.github_actions_allowed_subject_patterns
  ecr_repository_arns        = values(module.ecr.repository_arns)
  tags                       = local.common_tags
}
