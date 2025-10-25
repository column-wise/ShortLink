terraform {
  required_version = ">= 1.5"

  backend "s3" {
    bucket         = "shortlink-terraform-state-049759450795"
    key            = "dev/app/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    dynamodb_table = "shortlink-terraform-lock"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    template = {
      source  = "hashicorp/template"
      version = "~> 2.2"
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

# ────────────────────────────────────────────────────────────────────────────────
# Bootstrap 상태에서 VPC/서브넷/ECR 정보 가져오기
# ────────────────────────────────────────────────────────────────────────────────
data "terraform_remote_state" "bootstrap" {
  backend = "s3"
  config = {
    bucket = "shortlink-terraform-state-049759450795"
    key    = "dev/bootstrap/terraform.tfstate"
    region = "ap-northeast-2"
  }
}

# ────────────────────────────────────────────────────────────────────────────────
# Security Group (모듈)
# ────────────────────────────────────────────────────────────────────────────────
module "app_sg" {
  source      = "../../modules/security-group"
  environment = "dev"
  name        = "app"
  description = "Security group for application servers"
  vpc_id      = data.terraform_remote_state.bootstrap.outputs.vpc_id

  ingress_rules = [
    {
      protocol     = "tcp"
      from_port    = 22
      to_port      = 22
      cidr_blocks  = "0.0.0.0/0" # TODO: GitHub Actions egress IP로 제한 권장
      description  = "SSH"
    },
    {
      protocol     = "tcp"
      from_port    = 8080
      to_port      = 8080
      cidr_blocks  = "0.0.0.0/0"
      description  = "App HTTP"
    },
    {
      protocol     = "tcp"
      from_port    = 80
      to_port      = 80
      cidr_blocks  = "0.0.0.0/0"
      description  = "HTTP"
    },
    {
      protocol     = "tcp"
      from_port    = 443
      to_port      = 443
      cidr_blocks  = "0.0.0.0/0"
      description  = "HTTPS"
    }
  ]

  # 모든 아웃바운드 허용 (ports 지정하지 않음!)
  egress_rules = [
    {
      protocol     = "-1"          # all protocols
      cidr_blocks  = "0.0.0.0/0"
      description  = "Allow all outbound traffic"
    }
  ]
}

# ────────────────────────────────────────────────────────────────────────────────
# EC2용 IAM Role / Instance Profile
# ────────────────────────────────────────────────────────────────────────────────
resource "aws_iam_role" "ec2_role" {
  name = "shortlink-dev-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect    = "Allow",
      Action    = "sts:AssumeRole",
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

# ECR ReadOnly
resource "aws_iam_role_policy_attachment" "ecr_read_only" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# CloudWatch Logs/Agent(선택적)
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "shortlink-dev-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ────────────────────────────────────────────────────────────────────────────────
# EC2 Key Pair (기존 공개키 사용)
# ────────────────────────────────────────────────────────────────────────────────
resource "aws_key_pair" "dev" {
  key_name   = "shortlink-dev-key"
  public_key = var.ssh_public_key
}

# ────────────────────────────────────────────────────────────────────────────────
# User Data (템플릿)
# ────────────────────────────────────────────────────────────────────────────────
data "template_file" "user_data" {
  template = file("${path.module}/user-data.sh")
  vars = {
    ecr_repository_api      = data.terraform_remote_state.bootstrap.outputs.ecr_api_repository_url
    ecr_repository_consumer = data.terraform_remote_state.bootstrap.outputs.ecr_consumer_repository_url
    aws_region              = var.aws_region
  }
}

# ────────────────────────────────────────────────────────────────────────────────
# EC2 (모듈 호출)
# ────────────────────────────────────────────────────────────────────────────────
module "app_server" {
  source = "../../modules/ec2"

  environment          = "dev"
  name                 = "app"
  instance_type        = var.instance_type
  subnet_id            = data.terraform_remote_state.bootstrap.outputs.public_subnet_ids[0]

  security_group_ids   = [module.app_sg.security_group_id]
  key_name             = aws_key_pair.dev.key_name
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name

  user_data            = data.template_file.user_data.rendered
  associate_public_ip  = true
  root_volume_size     = 30
}
