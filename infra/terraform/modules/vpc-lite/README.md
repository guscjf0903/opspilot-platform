# VPC Lite Module

Creates a small, low-cost VPC foundation for ephemeral OpsPilot demos.

Cost guardrails:

- Uses 2 public subnets by default.
- Does not create NAT Gateway.
- Does not create VPC endpoints.
- Does not auto-assign public IPv4 addresses to launched instances.

This is a portfolio demo network, not a production network. A production-grade design should use private subnets, NAT Gateway or VPC endpoints, tighter ingress controls, and WAF where appropriate.
