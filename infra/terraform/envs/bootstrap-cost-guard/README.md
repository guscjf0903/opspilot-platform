# Bootstrap Cost Guard

This is the first Terraform environment to run for OpsPilot AWS work.

It creates only AWS Budgets notifications. It does not create EKS, EC2, ALB,
RDS, MSK, NAT Gateway, Route53, or any runtime workload.

## When to use

Run this before creating any other AWS resources.

## Setup

```bash
cd infra/terraform/envs/bootstrap-cost-guard
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` and set a real email address.

```hcl
alert_emails = ["you@example.com"]
```

Do not commit `terraform.tfvars`.

## Validate

```bash
terraform init -backend=false
terraform fmt -recursive
terraform validate
terraform plan
```

## Apply policy

Only run `terraform apply` after a human review of the plan output.

This environment is safe by design, but it still creates an AWS account-level
budget notification. Keep the approval habit from the first step.
