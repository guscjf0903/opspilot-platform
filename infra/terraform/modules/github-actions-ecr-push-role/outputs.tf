output "role_arn" {
  description = "IAM role ARN to store in the GitHub secret AWS_GITHUB_ACTIONS_ROLE_ARN."
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "IAM role name."
  value       = aws_iam_role.this.name
}

output "oidc_provider_arn" {
  description = "GitHub Actions OIDC provider ARN used by the role trust policy."
  value       = local.oidc_provider_arn
}
