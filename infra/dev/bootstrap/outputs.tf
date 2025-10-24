output "s3_bucket_name" {
  description = "Terraform state S3 bucket name"
  value       = aws_s3_bucket.terraform_state.bucket
}

output "dynamodb_table_name" {
  description = "Terraform lock DynamoDB table name"
  value       = aws_dynamodb_table.terraform_lock.name
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.vpc.private_subnet_ids
}

output "ecr_api_repository_url" {
  description = "ECR repository URL for API server"
  value       = aws_ecr_repository.api_server.repository_url
}

output "ecr_consumer_repository_url" {
  description = "ECR repository URL for events consumer"
  value       = aws_ecr_repository.events_consumer.repository_url
}
