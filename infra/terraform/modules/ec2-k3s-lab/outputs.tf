output "instance_id" {
  description = "EC2 instance ID."
  value       = aws_instance.this.id
}

output "public_ip" {
  description = "Public IPv4 address. Public IPv4 is billed while the instance is running."
  value       = aws_instance.this.public_ip
}

output "public_dns" {
  description = "Public DNS name."
  value       = aws_instance.this.public_dns
}

output "security_group_id" {
  description = "Security group ID."
  value       = aws_security_group.this.id
}

output "instance_profile_name" {
  description = "IAM instance profile name."
  value       = aws_iam_instance_profile.this.name
}

output "ssm_start_session_command" {
  description = "Command for starting an SSM shell session."
  value       = "aws ssm start-session --target ${aws_instance.this.id} --region ${data.aws_region.current.name}"
}

output "k3s_status_command" {
  description = "Command to check k3s status from an SSM shell."
  value       = "sudo systemctl status k3s --no-pager && kubectl get nodes"
}
