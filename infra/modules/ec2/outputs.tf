output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.main.id
}

output "private_ip" {
  description = "Private IP address"
  value       = aws_instance.main.private_ip
}

output "public_ip" {
  description = "Public IP address"
  value       = aws_instance.main.public_ip
}

output "availability_zone" {
  description = "Availability zone of the instance"
  value       = aws_instance.main.availability_zone
}
