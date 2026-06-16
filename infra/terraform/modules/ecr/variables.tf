variable "repository_names" {
  description = "Private ECR repository names to create."
  type        = list(string)

  validation {
    condition     = length(var.repository_names) > 0
    error_message = "At least one ECR repository name is required."
  }
}

variable "image_tag_mutability" {
  description = "ECR image tag mutability."
  type        = string
  default     = "IMMUTABLE"

  validation {
    condition     = contains(["MUTABLE", "IMMUTABLE"], var.image_tag_mutability)
    error_message = "image_tag_mutability must be MUTABLE or IMMUTABLE."
  }
}

variable "scan_on_push" {
  description = "Whether ECR scans images when they are pushed."
  type        = bool
  default     = false
}

variable "expire_untagged_after_days" {
  description = "Number of days before untagged images expire."
  type        = number
  default     = 3

  validation {
    condition     = var.expire_untagged_after_days >= 1 && var.expire_untagged_after_days <= 30
    error_message = "expire_untagged_after_days must be between 1 and 30."
  }
}

variable "keep_recent_images" {
  description = "Number of recent images to keep per repository."
  type        = number
  default     = 5

  validation {
    condition     = var.keep_recent_images >= 1 && var.keep_recent_images <= 20
    error_message = "keep_recent_images must be between 1 and 20."
  }
}

variable "tags" {
  description = "Common tags for AWS resources."
  type        = map(string)
  default     = {}
}
