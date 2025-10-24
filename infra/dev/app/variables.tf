variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.large"  # Upgrade for dev server
}

variable "ssh_public_key" {
  description = "SSH public key for EC2 access"
  type        = string
}

variable "state_bucket_name" {
  description = "Name of the S3 bucket storing Terraform remote state (from bootstrap output)"
  type        = string
}
