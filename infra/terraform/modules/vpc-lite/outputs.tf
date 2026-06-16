output "vpc_id" {
  description = "VPC ID."
  value       = aws_vpc.this.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR block."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "Public subnet IDs."
  value       = values(aws_subnet.public)[*].id
}

output "public_subnet_id_by_az" {
  description = "Public subnet IDs keyed by availability zone."
  value       = { for az, subnet in aws_subnet.public : az => subnet.id }
}

output "internet_gateway_id" {
  description = "Internet Gateway ID."
  value       = aws_internet_gateway.this.id
}
