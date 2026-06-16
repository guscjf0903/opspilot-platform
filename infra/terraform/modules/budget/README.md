# Budget Module

Creates a low-cost AWS Budgets guardrail for OpsPilot portfolio environments.

This module is intentionally small. It does not create EKS, RDS, ALB, NAT
Gateway, MSK, or any runtime infrastructure. Apply it before any other AWS
environment so cost notifications are already active.

## Inputs

| Name | Purpose |
| --- | --- |
| `budget_name` | Monthly budget name |
| `monthly_limit_usd` | Monthly cost limit in USD |
| `alert_emails` | Email recipients for budget notifications |
| `actual_thresholds_percent` | Actual spend thresholds |
| `forecasted_thresholds_percent` | Forecasted spend thresholds |
| `tags` | Common ownership and cost tags |

## Notes

- Do not put AWS credentials in Terraform files.
- Do not commit `terraform.tfvars`.
- Run `terraform plan` first and review the output before apply.
