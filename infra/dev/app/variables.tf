variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"  # Free Tier
}

variable "ssh_public_key" {
  description = "SSH public key for EC2 access"
  type        = string
}
