variable "name_prefix" {
  description = "Name prefix for EC2 k3s lab resources."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the k3s lab instance is launched."
  type        = string
}

variable "subnet_id" {
  description = "Public subnet ID for the k3s lab instance."
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type for the single-node k3s lab."
  type        = string
  default     = "t3.small"
}

variable "root_volume_size_gb" {
  description = "Root EBS volume size in GiB."
  type        = number
  default     = 20

  validation {
    condition     = var.root_volume_size_gb >= 20 && var.root_volume_size_gb <= 60
    error_message = "root_volume_size_gb must be between 20 and 60."
  }
}

variable "key_name" {
  description = "Optional EC2 key pair name. Leave null and use SSM Session Manager by default."
  type        = string
  default     = null
}

variable "allowed_ssh_cidr_blocks" {
  description = "CIDR blocks allowed to access SSH. Empty by default."
  type        = list(string)
  default     = []
}

variable "allowed_k3s_api_cidr_blocks" {
  description = "CIDR blocks allowed to access the Kubernetes API on 6443. Empty by default."
  type        = list(string)
  default     = []
}

variable "allowed_http_cidr_blocks" {
  description = "CIDR blocks allowed to access HTTP/HTTPS on the instance. Empty by default."
  type        = list(string)
  default     = []
}

variable "k3s_channel" {
  description = "k3s install channel."
  type        = string
  default     = "stable"
}

variable "ecr_repository_arns" {
  description = "ECR repositories that the instance can pull images from."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags applied to all resources."
  type        = map(string)
  default     = {}
}
