output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by repository name."
  value       = var.enable_ecr ? module.ecr[0].repository_urls : {}
}

output "vpc_id" {
  description = "VPC ID for the ephemeral demo network."
  value       = var.enable_vpc ? module.vpc_lite[0].vpc_id : null
}

output "public_subnet_ids" {
  description = "Public subnet IDs for the ephemeral demo network."
  value       = var.enable_vpc ? module.vpc_lite[0].public_subnet_ids : []
}

output "expensive_feature_flags" {
  description = "Flags for resources that are intentionally disabled by default."
  value = {
    enable_eks         = var.enable_eks
    enable_alb         = var.enable_alb
    enable_rds         = var.enable_rds
    enable_msk         = var.enable_msk
    enable_nat_gateway = var.enable_nat_gateway
  }
}
