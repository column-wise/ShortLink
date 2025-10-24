output "instance_id" {
  description = "EC2 instance ID"
  value       = module.app_server.instance_id
}

output "instance_public_ip" {
  description = "EC2 instance public IP"
  value       = module.app_server.public_ip
}

output "instance_private_ip" {
  description = "EC2 instance private IP"
  value       = module.app_server.private_ip
}

output "security_group_id" {
  description = "Security group ID"
  value       = module.app_sg.security_group_id
}

output "ssh_command" {
  description = "SSH command to connect to the instance"
  value       = "ssh -i ~/.ssh/shortlink-dev-key ec2-user@${module.app_server.public_ip}"
}
