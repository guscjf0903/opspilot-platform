variable "budget_name" {
  description = "Name of the monthly cost budget."
  type        = string
}

variable "monthly_limit_usd" {
  description = "Monthly budget limit in USD."
  type        = number

  validation {
    condition     = var.monthly_limit_usd > 0
    error_message = "monthly_limit_usd must be greater than 0."
  }
}

variable "currency" {
  description = "Budget currency."
  type        = string
  default     = "USD"
}

variable "alert_emails" {
  description = "Email addresses that receive AWS Budgets notifications."
  type        = list(string)

  validation {
    condition     = length(var.alert_emails) > 0
    error_message = "At least one alert email is required."
  }
}

variable "actual_thresholds_percent" {
  description = "Actual spend thresholds as percentages of the monthly budget."
  type        = list(number)
  default     = [50, 80, 100]
}

variable "forecasted_thresholds_percent" {
  description = "Forecasted spend thresholds as percentages of the monthly budget."
  type        = list(number)
  default     = [80, 100]
}

variable "tags" {
  description = "Common tags for AWS resources."
  type        = map(string)
  default     = {}
}
