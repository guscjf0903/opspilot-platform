output "ecr_repository_urls" {
  description = "Foundation ECR repository URLs keyed by repository name."
  value       = var.foundation_ecr_repository_urls
}

output "ecr_repository_arns" {
  description = "Foundation ECR repository ARNs keyed by repository name."
  value       = var.foundation_ecr_repository_arns
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

output "eks" {
  description = "Mode B EKS connection details. Null when disabled."
  value = var.enable_eks && var.enable_vpc ? {
    cluster_name              = module.eks_ephemeral[0].cluster_name
    cluster_arn               = module.eks_ephemeral[0].cluster_arn
    cluster_endpoint          = module.eks_ephemeral[0].cluster_endpoint
    node_group_name           = module.eks_ephemeral[0].node_group_name
    node_role_arn             = module.eks_ephemeral[0].node_role_arn
    update_kubeconfig_command = module.eks_ephemeral[0].update_kubeconfig_command
  } : null
}

output "eks_cluster_name" {
  description = "Mode B EKS cluster name. Empty when disabled."
  value       = var.enable_eks && var.enable_vpc ? module.eks_ephemeral[0].cluster_name : ""
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
