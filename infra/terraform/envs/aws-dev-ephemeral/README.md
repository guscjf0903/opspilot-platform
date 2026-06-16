# AWS Dev Ephemeral

This environment is the short-lived AWS demo foundation for OpsPilot.

Current scope:

- Private ECR repositories for backend and frontend images.
- A small VPC with 2 public subnets.
- No NAT Gateway.
- No EKS, ALB, RDS, or MSK yet.

The goal is to prepare low-cost building blocks before adding expensive demo resources.

## Safety Defaults

```text
enable_ecr         = true
enable_vpc         = true
enable_eks         = false
enable_alb         = false
enable_rds         = false
enable_msk         = false
enable_nat_gateway = false
default_ttl_hours  = 4
```

Do not run `terraform apply` until the AWS account, monthly budget, and expected destroy time are confirmed.

## Validate

```bash
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral init -backend=false
terraform -chdir=infra/terraform/envs/aws-dev-ephemeral validate
```
