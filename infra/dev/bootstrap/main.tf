terraform {
  required_version = ">= 1.5"

  # Bootstrap 상태 저장을 위한 S3 백엔드
  backend "local" {
    path = "./terraform.tfstate"
  }

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

# S3 버킷 - Terraform 상태 저장용
resource "aws_s3_bucket" "terraform_state" {
  bucket = "shortlink-terraform-state-${var.aws_account_id}"

  force_destroy = true
  lifecycle {
    # prevent_destroy = true  # 실수 삭제 방지
  }
}

# S3 버킷 버전 관리
resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  versioning_configuration {
    status = "Enabled"
  }
}

# S3 버킷 서버측 암호화
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# S3 버킷 퍼블릭 액세스 차단
resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# (DynamoDB 상태 잠금은 개인 프로젝트에서는 생략)

# VPC
module "vpc" {
  source = "../../modules/vpc"

  environment = "dev"
  cidr_block  = var.vpc_cidr
}

# ECR Repositories
resource "aws_ecr_repository" "api_server" {
  name                 = "shortlink-api"
  image_tag_mutability = "MUTABLE"
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  lifecycle {
    prevent_destroy = false
  }
}

resource "aws_ecr_repository" "events_consumer" {
  name                 = "shortlink-events-consumer"
  image_tag_mutability = "MUTABLE"
  force_delete = true

  image_scanning_configuration {
    scan_on_push = true
  }

  lifecycle {
    prevent_destroy = false
  }
}

# ECR Lifecycle Policy - 오래된 이미지 자동 정리
resource "aws_ecr_lifecycle_policy" "api_server" {
  repository = aws_ecr_repository.api_server.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}

resource "aws_ecr_lifecycle_policy" "events_consumer" {
  repository = aws_ecr_repository.events_consumer.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}
