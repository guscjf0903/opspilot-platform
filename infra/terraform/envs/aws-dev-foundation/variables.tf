variable "aws_region" {
  description = "AWS region for provider configuration."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used in resource names and tags."
  type        = string
  default     = "opspilot"
}

variable "environment" {
  description = "Environment name for tags."
  type        = string
  default     = "aws-dev"
}

variable "default_ttl_hours" {
  description = "Expected maximum lifetime for ephemeral demo resources. Kept as a tag for shared cost visibility."
  type        = number
  default     = 4

  validation {
    condition     = var.default_ttl_hours >= 1 && var.default_ttl_hours <= 8
    error_message = "default_ttl_hours must be between 1 and 8."
  }
}

variable "ecr_repository_names" {
  description = "Private ECR repositories for OpsPilot images."
  type        = list(string)
  default     = ["opspilot-backend", "opspilot-frontend"]

  validation {
    condition     = length(var.ecr_repository_names) > 0
    error_message = "ecr_repository_names must contain at least one repository name."
  }
}

variable "ecr_image_tag_mutability" {
  description = "ECR image tag mutability."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.ecr_image_tag_mutability)
    error_message = "ecr_image_tag_mutability must be MUTABLE or IMMUTABLE."
  }
}

variable "ecr_scan_on_push" {
  description = "Whether ECR scans images when they are pushed."
  type        = bool
  default     = false
}

variable "ecr_expire_untagged_after_days" {
  description = "Number of days before untagged ECR images expire."
  type        = number
  default     = 3
}

variable "ecr_keep_recent_images" {
  description = "Number of recent ECR images to keep."
  type        = number
  default     = 5
}

variable "github_actions_ecr_push_role_name" {
  description = "IAM role name for GitHub Actions ECR push."
  type        = string
  default     = "opspilot-aws-dev-github-actions-ecr-push"
}

variable "create_github_oidc_provider" {
  description = "Create GitHub Actions OIDC provider. Set false when the AWS account already has one."
  type        = bool
  default     = true
}

variable "existing_github_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN when create_github_oidc_provider is false."
  type        = string
  default     = ""
}

variable "github_actions_allowed_subject_patterns" {
  description = "GitHub OIDC sub patterns that can assume the ECR push role."
  type        = list(string)
  default     = ["repo:guscjf0903/opspilot-platform:ref:refs/heads/main"]

  validation {
    condition     = length(var.github_actions_allowed_subject_patterns) > 0
    error_message = "github_actions_allowed_subject_patterns must contain at least one subject pattern."
  }
}

variable "extra_tags" {
  description = "Additional tags to merge into all resources."
  type        = map(string)
  default     = {}
}
