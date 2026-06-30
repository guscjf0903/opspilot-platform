variable "aws_region" {
  description = "AWS region for provider configuration."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used in resource names and tags."
  type        = string
  default     = "opspilot"
}

variable "environment" {
  description = "Environment name for tags."
  type        = string
  default     = "aws-dev"
}

variable "default_ttl_hours" {
  description = "Expected maximum lifetime for ephemeral demo resources."
  type        = number
  default     = 4

  validation {
    condition     = var.default_ttl_hours >= 1 && var.default_ttl_hours <= 8
    error_message = "default_ttl_hours must be between 1 and 8."
  }
}

variable "enable_ecr" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = bool
  default     = false
}

variable "enable_vpc" {
  description = "Create the low-cost VPC foundation."
  type        = bool
  default     = true
}

variable "enable_eks" {
  description = "Create short-lived EKS demo resources. Disabled by default because EKS creates billable resources."
  type        = bool
  default     = false
}

variable "enable_alb" {
  description = "Reserved for optional public live demo ingress. Disabled by default."
  type        = bool
  default     = false
}

variable "enable_rds" {
  description = "Reserved for optional RDS proof. Disabled by default."
  type        = bool
  default     = false
}

variable "enable_msk" {
  description = "Reserved for optional MSK proof. Disabled by default."
  type        = bool
  default     = false
}

variable "enable_nat_gateway" {
  description = "Reserved for production-like private subnet networking. Disabled by default."
  type        = bool
  default     = false
}

variable "enable_k3s_lab" {
  description = "Create a single EC2 instance with k3s for short Mode B Lite live demos. Disabled by default because it creates billable resources."
  type        = bool
  default     = false
}

variable "eks_cluster_name" {
  description = "Optional EKS cluster name. Defaults to <project>-<environment>-eks."
  type        = string
  default     = null
}

variable "eks_kubernetes_version" {
  description = "Optional EKS Kubernetes version. Leave null to use the AWS default for the region/account."
  type        = string
  default     = null
}

variable "eks_endpoint_public_access" {
  description = "Whether the EKS API endpoint is reachable from the public internet."
  type        = bool
  default     = true
}

variable "eks_endpoint_private_access" {
  description = "Whether the EKS API endpoint is reachable from inside the VPC."
  type        = bool
  default     = false
}

variable "eks_endpoint_public_access_cidrs" {
  description = "CIDR blocks allowed to access the public EKS API endpoint. Restrict this to your current IP for real demos when possible."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "eks_node_group_instance_types" {
  description = "EC2 instance types for the EKS managed node group."
  type        = list(string)
  default     = ["t3.medium"]
}

variable "eks_node_group_ami_type" {
  description = "AMI type for the EKS managed node group."
  type        = string
  default     = "AL2023_x86_64_STANDARD"
}

variable "eks_node_group_capacity_type" {
  description = "Capacity type for the EKS managed node group."
  type        = string
  default     = "ON_DEMAND"
}

variable "eks_node_group_desired_size" {
  description = "Desired node count for the EKS managed node group."
  type        = number
  default     = 1
}

variable "eks_node_group_min_size" {
  description = "Minimum node count for the EKS managed node group."
  type        = number
  default     = 0
}

variable "eks_node_group_max_size" {
  description = "Maximum node count for the EKS managed node group."
  type        = number
  default     = 1
}

variable "eks_node_group_disk_size_gb" {
  description = "Root disk size for each EKS managed node group instance."
  type        = number
  default     = 20
}

variable "enable_github_actions_ecr_push_role" {
  description = "Deprecated. GitHub Actions ECR push role is managed by aws-dev-foundation."
  type        = bool
  default     = false
}

variable "ecr_repository_names" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = list(string)
  default     = ["opspilot-backend", "opspilot-frontend"]
}

variable "ecr_image_tag_mutability" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.ecr_image_tag_mutability)
    error_message = "ecr_image_tag_mutability must be MUTABLE or IMMUTABLE."
  }
}

variable "ecr_scan_on_push" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = bool
  default     = false
}

variable "ecr_expire_untagged_after_days" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = number
  default     = 3
}

variable "ecr_keep_recent_images" {
  description = "Deprecated. ECR repositories are managed by aws-dev-foundation."
  type        = number
  default     = 5
}

variable "github_actions_ecr_push_role_name" {
  description = "Deprecated. GitHub Actions ECR push role is managed by aws-dev-foundation."
  type        = string
  default     = "opspilot-aws-dev-github-actions-ecr-push"
}

variable "create_github_oidc_provider" {
  description = "Deprecated. GitHub Actions ECR push role is managed by aws-dev-foundation."
  type        = bool
  default     = true
}

variable "existing_github_oidc_provider_arn" {
  description = "Deprecated. GitHub Actions ECR push role is managed by aws-dev-foundation."
  type        = string
  default     = ""
}

variable "github_actions_allowed_subject_patterns" {
  description = "Deprecated. GitHub Actions ECR push role is managed by aws-dev-foundation."
  type        = list(string)
  default     = ["repo:guscjf0903/opspilot-platform:ref:refs/heads/main"]

  validation {
    condition     = length(var.github_actions_allowed_subject_patterns) > 0
    error_message = "github_actions_allowed_subject_patterns must contain at least one subject pattern."
  }
}

variable "foundation_ecr_repository_arns" {
  description = "ECR repository ARNs from aws-dev-foundation, keyed by repository name."
  type        = map(string)
  default     = {}
}

variable "foundation_ecr_repository_urls" {
  description = "ECR repository URLs from aws-dev-foundation, keyed by repository name."
  type        = map(string)
  default     = {}
}

variable "vpc_cidr_block" {
  description = "CIDR block for the low-cost demo VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) == 2
    error_message = "aws-dev-ephemeral uses exactly 2 public subnets to keep the demo simple."
  }
}

variable "k3s_lab_subnet_index" {
  description = "Index of the public subnet used by the k3s lab instance."
  type        = number
  default     = 0

  validation {
    condition     = var.k3s_lab_subnet_index >= 0 && var.k3s_lab_subnet_index < 2
    error_message = "k3s_lab_subnet_index must be 0 or 1."
  }
}

variable "k3s_lab_instance_type" {
  description = "EC2 instance type for the k3s lab. t3.small is the default balance between low cost and enough memory for the Java backend."
  type        = string
  default     = "t3.small"
}

variable "k3s_lab_root_volume_size_gb" {
  description = "Root EBS volume size for the k3s lab instance."
  type        = number
  default     = 20
}

variable "k3s_lab_key_name" {
  description = "Optional EC2 key pair name. Leave null to avoid SSH and use SSM Session Manager."
  type        = string
  default     = null
}

variable "k3s_lab_allowed_ssh_cidr_blocks" {
  description = "CIDR blocks allowed to SSH into the k3s lab. Empty by default."
  type        = list(string)
  default     = []
}

variable "k3s_lab_allowed_k3s_api_cidr_blocks" {
  description = "CIDR blocks allowed to access the k3s API server. Empty by default."
  type        = list(string)
  default     = []
}

variable "k3s_lab_allowed_http_cidr_blocks" {
  description = "CIDR blocks allowed to access HTTP/HTTPS on the k3s lab. Empty by default."
  type        = list(string)
  default     = []
}

variable "k3s_lab_k3s_channel" {
  description = "k3s install channel."
  type        = string
  default     = "stable"
}

variable "extra_tags" {
  description = "Additional tags to merge into all resources."
  type        = map(string)
  default     = {}
}
