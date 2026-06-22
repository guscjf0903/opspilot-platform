output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by repository name."
  value       = var.enable_ecr ? module.ecr[0].repository_urls : {}
}

output "github_actions_ecr_push_role_arn" {
  description = "IAM role ARN to store in GitHub secret AWS_GITHUB_ACTIONS_ROLE_ARN."
  value       = var.enable_github_actions_ecr_push_role && var.enable_ecr ? module.github_actions_ecr_push_role[0].role_arn : null
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
    enable_k3s_lab     = var.enable_k3s_lab
    enable_eks         = var.enable_eks
    enable_alb         = var.enable_alb
    enable_rds         = var.enable_rds
    enable_msk         = var.enable_msk
    enable_nat_gateway = var.enable_nat_gateway
  }
}

output "k3s_lab" {
  description = "Mode B Lite single-node k3s lab connection details. Null when disabled."
  value = var.enable_k3s_lab && var.enable_vpc ? {
    instance_id               = module.k3s_lab[0].instance_id
    public_ip                 = module.k3s_lab[0].public_ip
    public_dns                = module.k3s_lab[0].public_dns
    security_group_id         = module.k3s_lab[0].security_group_id
    ssm_start_session_command = module.k3s_lab[0].ssm_start_session_command
    k3s_status_command        = module.k3s_lab[0].k3s_status_command
  } : null
}

output "k3s_lab_instance_id" {
  description = "Mode B Lite EC2 instance ID for SSM scripts. Empty when disabled."
  value       = var.enable_k3s_lab && var.enable_vpc ? module.k3s_lab[0].instance_id : ""
}
