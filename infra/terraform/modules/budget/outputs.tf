output "budget_name" {
  description = "AWS Budgets budget name."
  value       = aws_budgets_budget.monthly_cost.name
}

output "budget_limit_usd" {
  description = "Monthly budget limit in USD."
  value       = var.monthly_limit_usd
}
