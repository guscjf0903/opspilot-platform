variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
}

variable "kubernetes_version" {
  description = "Optional EKS Kubernetes version. Leave null to use the AWS default for the region/account."
  type        = string
  default     = null
}

variable "public_subnet_ids" {
  description = "Public subnet IDs used by the EKS control plane and managed node group."
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_ids) >= 2
    error_message = "At least two public subnets are required for EKS."
  }
}

variable "public_subnet_id_map" {
  description = "Public subnet IDs keyed by a stable name such as availability zone. Keys must be known during plan."
  type        = map(string)

  validation {
    condition     = length(var.public_subnet_id_map) >= 2
    error_message = "At least two public subnets are required for EKS."
  }
}

variable "endpoint_public_access" {
  description = "Whether the EKS API endpoint is reachable from the public internet."
  type        = bool
  default     = true
}

variable "endpoint_private_access" {
  description = "Whether the EKS API endpoint is reachable from inside the VPC."
  type        = bool
  default     = false
}

variable "endpoint_public_access_cidrs" {
  description = "CIDR blocks allowed to access the public EKS API endpoint."
  type        = list(string)
  default     = ["0.0.0.0/0"]

  validation {
    condition     = length(var.endpoint_public_access_cidrs) > 0
    error_message = "endpoint_public_access_cidrs must contain at least one CIDR block."
  }
}

variable "node_group_instance_types" {
  description = "EC2 instance types for the managed node group."
  type        = list(string)
  default     = ["t3.medium"]

  validation {
    condition     = length(var.node_group_instance_types) > 0
    error_message = "node_group_instance_types must contain at least one instance type."
  }
}

variable "node_group_ami_type" {
  description = "AMI type for the managed node group."
  type        = string
  default     = "AL2023_x86_64_STANDARD"
}

variable "node_group_capacity_type" {
  description = "Capacity type for the managed node group."
  type        = string
  default     = "ON_DEMAND"

  validation {
    condition     = contains(["ON_DEMAND", "SPOT"], var.node_group_capacity_type)
    error_message = "node_group_capacity_type must be ON_DEMAND or SPOT."
  }
}

variable "node_group_desired_size" {
  description = "Desired node count for the managed node group."
  type        = number
  default     = 1

  validation {
    condition     = var.node_group_desired_size >= 0 && var.node_group_desired_size <= 2
    error_message = "node_group_desired_size must be between 0 and 2 for the low-cost lab."
  }
}

variable "node_group_min_size" {
  description = "Minimum node count for the managed node group."
  type        = number
  default     = 0

  validation {
    condition     = var.node_group_min_size >= 0 && var.node_group_min_size <= 2
    error_message = "node_group_min_size must be between 0 and 2 for the low-cost lab."
  }
}

variable "node_group_max_size" {
  description = "Maximum node count for the managed node group."
  type        = number
  default     = 1

  validation {
    condition     = var.node_group_max_size >= 1 && var.node_group_max_size <= 2
    error_message = "node_group_max_size must be between 1 and 2 for the low-cost lab."
  }
}

variable "node_group_disk_size_gb" {
  description = "Root disk size for each managed node group instance."
  type        = number
  default     = 20

  validation {
    condition     = var.node_group_disk_size_gb >= 20 && var.node_group_disk_size_gb <= 80
    error_message = "node_group_disk_size_gb must be between 20 and 80."
  }
}

variable "node_group_labels" {
  description = "Kubernetes labels applied to managed node group nodes."
  type        = map(string)
  default = {
    "opspilot.io/mode"    = "aws-dev-ephemeral"
    "opspilot.io/runtime" = "eks"
  }
}

variable "tags" {
  description = "Tags applied to all resources."
  type        = map(string)
  default     = {}
}
