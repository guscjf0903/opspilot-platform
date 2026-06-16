locals {
  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
    CostMode    = "portfolio-low-cost"
  }
}

module "monthly_budget" {
  source = "../../modules/budget"

  budget_name       = "${var.project_name}-${var.environment}-monthly-cost"
  monthly_limit_usd = var.monthly_budget_usd
  alert_emails      = var.alert_emails
  tags              = local.common_tags
}
