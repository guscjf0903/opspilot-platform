variable "aws_region" {
  description = "AWS region for provider configuration."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used in resource names."
  type        = string
  default     = "opspilot"
}

variable "environment" {
  description = "Environment name for tags and budget naming."
  type        = string
  default     = "bootstrap"
}

variable "monthly_budget_usd" {
  description = "Monthly budget limit in USD."
  type        = number
  default     = 5

  validation {
    condition     = var.monthly_budget_usd > 0 && var.monthly_budget_usd <= 10
    error_message = "For portfolio safety, monthly_budget_usd must be between 1 and 10."
  }
}

variable "alert_emails" {
  description = "Email addresses that receive AWS Budgets notifications."
  type        = list(string)

  validation {
    condition     = length(var.alert_emails) > 0
    error_message = "At least one alert email is required."
  }
}
