output "ecr_repository_arns" {
  description = "ECR repository ARNs keyed by repository name. Pass this to aws-dev-ephemeral."
  value       = module.ecr.repository_arns
}

output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by repository name. Pass this to aws-dev-ephemeral and GitHub Actions."
  value       = module.ecr.repository_urls
}

output "github_actions_ecr_push_role_arn" {
  description = "IAM role ARN to store in the GitHub secret AWS_GITHUB_ACTIONS_ROLE_ARN."
  value       = module.github_actions_ecr_push_role.role_arn
}

output "github_actions_ecr_push_role_name" {
  description = "IAM role name for GitHub Actions ECR push."
  value       = module.github_actions_ecr_push_role.role_name
}

output "github_oidc_provider_arn" {
  description = "GitHub Actions OIDC provider ARN used by the role trust policy."
  value       = module.github_actions_ecr_push_role.oidc_provider_arn
}
