variable "name_prefix" {
  description = "Prefix used in resource names."
  type        = string
}

variable "cidr_block" {
  description = "VPC CIDR block."
  type        = string
  default     = "10.40.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]

  validation {
    condition     = length(var.public_subnet_cidrs) >= 2 && length(var.public_subnet_cidrs) <= 3
    error_message = "public_subnet_cidrs must contain 2 or 3 CIDR blocks for the portfolio demo."
  }
}

variable "availability_zone_names" {
  description = "Availability zone names to use for public subnets."
  type        = list(string)

  validation {
    condition     = length(var.availability_zone_names) >= 2 && length(var.availability_zone_names) <= 3
    error_message = "availability_zone_names must contain 2 or 3 AZ names for the portfolio demo."
  }
}

variable "map_public_ip_on_launch" {
  description = "Assign public IPv4 addresses to instances launched into public subnets. Required for NAT-free EKS worker nodes."
  type        = bool
  default     = false
}

variable "tags" {
  description = "Common tags for AWS resources."
  type        = map(string)
  default     = {}
}
