resource "aws_budgets_budget" "monthly_cost" {
  name         = var.budget_name
  budget_type  = "COST"
  limit_amount = tostring(var.monthly_limit_usd)
  limit_unit   = var.currency
  time_unit    = "MONTHLY"
  tags         = var.tags

  dynamic "notification" {
    for_each = toset(var.actual_thresholds_percent)

    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = "ACTUAL"
      subscriber_email_addresses = var.alert_emails
    }
  }

  dynamic "notification" {
    for_each = toset(var.forecasted_thresholds_percent)

    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = "FORECASTED"
      subscriber_email_addresses = var.alert_emails
    }
  }
}
