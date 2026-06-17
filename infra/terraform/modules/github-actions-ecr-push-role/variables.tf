variable "role_name" {
  description = "IAM role name assumed by GitHub Actions."
  type        = string

  validation {
    condition     = length(var.role_name) >= 3 && length(var.role_name) <= 64
    error_message = "role_name must be between 3 and 64 characters."
  }
}

variable "github_oidc_provider_url" {
  description = "GitHub Actions OIDC provider URL."
  type        = string
  default     = "https://token.actions.githubusercontent.com"
}

variable "oidc_audience" {
  description = "OIDC audience used by aws-actions/configure-aws-credentials."
  type        = string
  default     = "sts.amazonaws.com"
}

variable "create_oidc_provider" {
  description = "Create the GitHub Actions OIDC provider in this AWS account."
  type        = bool
  default     = true
}

variable "existing_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN. Required when create_oidc_provider is false."
  type        = string
  default     = ""
}

variable "allowed_subject_patterns" {
  description = "Allowed token.actions.githubusercontent.com:sub patterns for assuming this role."
  type        = list(string)

  validation {
    condition     = length(var.allowed_subject_patterns) > 0
    error_message = "allowed_subject_patterns must contain at least one GitHub OIDC subject pattern."
  }
}

variable "ecr_repository_arns" {
  description = "ECR repository ARNs that GitHub Actions can push to."
  type        = list(string)

  validation {
    condition     = length(var.ecr_repository_arns) > 0
    error_message = "ecr_repository_arns must contain at least one repository ARN."
  }
}

variable "max_session_duration_seconds" {
  description = "Maximum duration for assumed role sessions."
  type        = number
  default     = 3600

  validation {
    condition     = var.max_session_duration_seconds >= 900 && var.max_session_duration_seconds <= 43200
    error_message = "max_session_duration_seconds must be between 900 and 43200."
  }
}

variable "tags" {
  description = "Tags for IAM resources."
  type        = map(string)
  default     = {}
}
