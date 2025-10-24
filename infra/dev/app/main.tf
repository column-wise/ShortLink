terraform {
  required_version = ">= 1.5"

  # Backend은 `terraform init -backend-config` 플래그로 설정합니다.
  backend "s3" {}

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Environment = "dev"
      Project     = "shortlink"
      ManagedBy   = "terraform"
    }
  }
}

# Bootstrap 상태에서 VPC/네트워크 및 ECR 정보 가져오기
data "terraform_remote_state" "bootstrap" {
  backend = "s3"

  config = {
    bucket = var.state_bucket_name
    key    = "dev/bootstrap/terraform.tfstate"
    region = "ap-northeast-2"
  }
}

# EC2용 Security Group
module "app_sg" {
  source = "../../modules/security-group"

  environment = "dev"
  name        = "app"
  description = "Security group for application servers"
  vpc_id      = data.terraform_remote_state.bootstrap.outputs.vpc_id

  ingress_rules = [
    {
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"  # TODO: GitHub Actions IP로 제한 권장
      description = "SSH from anywhere"
    },
    {
      from_port   = 8080
      to_port     = 8080
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
      description = "HTTP application port"
    },
    {
      from_port   = 80
      to_port     = 80
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
      description = "HTTP"
    },
    {
      from_port   = 443
      to_port     = 443
      protocol    = "tcp"
      cidr_blocks = "0.0.0.0/0"
      description = "HTTPS"
    }
  ]

  egress_rules = [{
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = "0.0.0.0/0"
    description = "Allow all outbound traffic"
  }]
}

# EC2용 IAM Role (ECR 접근 권한)
resource "aws_iam_role" "ec2_role" {
  name = "shortlink-dev-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

# IAM Policy Attachment - ECR 읽기 권한
resource "aws_iam_role_policy_attachment" "ecr_read_only" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# IAM Policy Attachment - CloudWatch Logs
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

# IAM Instance Profile
resource "aws_iam_instance_profile" "ec2_profile" {
  name = "shortlink-dev-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# EC2 키 페어 (기존 키 사용 시 생략 가능)
resource "aws_key_pair" "dev" {
  key_name   = "shortlink-dev-key"
  public_key = var.ssh_public_key  # terraform.tfvars에서 입력
}

# User Data Script
data "template_file" "user_data" {
  template = file("${path.module}/user-data.sh")

  vars = {
    ecr_repository_api      = data.terraform_remote_state.bootstrap.outputs.ecr_api_repository_url
    ecr_repository_consumer = data.terraform_remote_state.bootstrap.outputs.ecr_consumer_repository_url
    aws_region              = var.aws_region
  }
}

# EC2 Instance
module "app_server" {
  source = "../../modules/ec2"

  environment = "dev"
  name        = "app"

  instance_type = var.instance_type
  subnet_id     = data.terraform_remote_state.bootstrap.outputs.public_subnet_ids[0]

  security_group_ids   = [module.app_sg.security_group_id]
  key_name             = aws_key_pair.dev.key_name
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name

  user_data = data.template_file.user_data.rendered

  associate_public_ip = true
  root_volume_size    = 30
}
