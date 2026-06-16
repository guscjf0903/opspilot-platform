output "budget_name" {
  description = "Created AWS Budgets budget name."
  value       = module.monthly_budget.budget_name
}

output "budget_limit_usd" {
  description = "Configured monthly budget limit in USD."
  value       = module.monthly_budget.budget_limit_usd
}
