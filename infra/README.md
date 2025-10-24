# ShortLink Infrastructure as Code

Terraform으로 관리하는 ShortLink 프로젝트 인프라

## 📁 구조

```
infra/
├── dev/                  # 개발 환경
│   ├── bootstrap/        # 기반 인프라 (S3, VPC, ECR)
│   └── app/              # 애플리케이션 인프라 (EC2)
│
├── prod/                 # 프로덕션 환경
│   ├── bootstrap/
│   └── app/
│
└── modules/              # 재사용 가능한 모듈
    ├── vpc/
    ├── ec2/
    ├── security-group/
    ├── ecr/
    ├── rds/
    ├── elasticache/
    └── alb/
```

## 🚀 시작하기

### 사전 준비

1. **AWS CLI 설치 및 설정**
```bash
# AWS CLI 설치 (macOS)
brew install awscli

# AWS 자격 증명 설정
aws configure
# AWS Access Key ID: [your-access-key]
# AWS Secret Access Key: [your-secret-key]
# Default region name: us-east-1
# Default output format: json

# 계정 ID 확인
aws sts get-caller-identity --query Account --output text
```

2. **Terraform 설치**
```bash
# macOS
brew install terraform

# 버전 확인 (1.5 이상 필요)
terraform version
```

3. **SSH 키 생성**
```bash
# EC2 접속용 SSH 키 생성
ssh-keygen -t rsa -b 4096 -f ~/.ssh/shortlink-dev-key

# 공개키 확인 (이 값을 terraform.tfvars에 입력)
cat ~/.ssh/shortlink-dev-key.pub
```

---

## 📝 배포 순서

### Step 1: Bootstrap 인프라 배포

Bootstrap은 **한 번만 실행**하여 기반 인프라를 생성합니다.

```bash
cd infra/dev/bootstrap

# 1. terraform.tfvars 수정
# - aws_account_id: 실제 AWS 계정 ID
vi terraform.tfvars

# 2. Terraform 초기화
terraform init

# 3. 실행 계획 확인
terraform plan

# 4. 인프라 생성
terraform apply

# 5. Output 확인
terraform output
```

**생성되는 리소스:**
- S3 버킷 (Terraform 상태 저장)
- DynamoDB 테이블 (상태 락)
- VPC, 서브넷, Internet Gateway, NAT Gateway
- ECR 리포지토리 (API, Events Consumer)

**중요:** Bootstrap apply 후 출력되는 S3 버킷명을 복사해서 `dev/app/main.tf`의 backend 설정에 입력하세요.

---

### Step 2: Application 인프라 배포

```bash
cd infra/dev/app

# 1. terraform.tfvars 수정
# - ssh_public_key: cat ~/.ssh/shortlink-dev-key.pub 의 출력값
vi terraform.tfvars

# 2. main.tf의 backend 설정 수정
# - bucket: bootstrap output의 s3_bucket_name 값
vi main.tf

# 3. Terraform 초기화 (S3 백엔드 사용)
terraform init

# 4. 실행 계획 확인
terraform plan

# 5. 인프라 생성
terraform apply

# 6. Output 확인 (Public IP 확인)
terraform output
```

**생성되는 리소스:**
- EC2 인스턴스 (t2.micro)
- Security Group (SSH, HTTP, HTTPS)
- IAM Role (ECR 접근 권한)
- Key Pair

---

## 🖥️ EC2 접속 및 애플리케이션 배포

### 1. SSH 접속

```bash
# Public IP 확인
cd infra/dev/app
terraform output instance_public_ip

# SSH 접속
ssh -i ~/.ssh/shortlink-dev-key ec2-user@<PUBLIC_IP>
```

### 2. 애플리케이션 배포

```bash
# EC2 서버 내부에서 실행

# 1. 프로젝트 클론
cd /opt/shortlink
git clone https://github.com/your-org/short-link.git
cd short-link/BE

# 2. ECR 로그인
/usr/local/bin/ecr-login.sh

# 3. 환경 변수 파일 생성
cp /opt/shortlink/.env.template .env

# 4. Docker Compose로 전체 스택 실행
docker-compose up -d

# 5. 로그 확인
docker-compose logs -f api-server
```

### 3. 헬스체크

```bash
# 로컬에서 실행
PUBLIC_IP=$(cd infra/dev/app && terraform output -raw instance_public_ip)

curl http://$PUBLIC_IP:8080/actuator/health
# 응답: {"status":"UP"}
```

---

## 🔧 주요 명령어

### Terraform

```bash
# 변경 사항 확인
terraform plan

# 인프라 적용
terraform apply

# 특정 리소스만 적용
terraform apply -target=module.app_server

# 리소스 삭제
terraform destroy

# Output 확인
terraform output

# 상태 확인
terraform state list
terraform state show <RESOURCE>
```

### AWS CLI

```bash
# ECR 리포지토리 목록
aws ecr describe-repositories

# ECR 이미지 목록
aws ecr list-images --repository-name shortlink-api

# EC2 인스턴스 목록
aws ec2 describe-instances --filters "Name=tag:Project,Values=shortlink"

# VPC 목록
aws ec2 describe-vpcs --filters "Name=tag:Project,Values=shortlink"
```

---

## 🗑️ 리소스 삭제

**주의:** 삭제 순서를 지켜야 합니다!

```bash
# 1. App 인프라 삭제
cd infra/dev/app
terraform destroy

# 2. Bootstrap 인프라 삭제
cd ../bootstrap
terraform destroy
```

**참고:** S3 버킷과 DynamoDB 테이블은 `prevent_destroy = true`로 보호되어 있습니다. 삭제하려면 main.tf에서 해당 설정을 제거해야 합니다.

---

## 📊 비용 예상

### 개발 환경 (us-east-1)

| 리소스 | 사양 | 월 비용 |
|--------|------|---------|
| EC2 | t2.micro (750시간 Free Tier) | **$0** |
| EBS | 30GB gp3 | $2.40 |
| NAT Gateway | 1개 | $32.40 |
| Data Transfer | 100GB 이내 | **$0** |
| S3 | Terraform 상태 (< 1GB) | $0.02 |
| **총계** | | **~$35/월** |

**비용 절감 팁:**
- 사용하지 않을 때 EC2 중지: `aws ec2 stop-instances --instance-ids <INSTANCE_ID>`
- NAT Gateway 제거 (Public Subnet만 사용): $32 절감
- 업무 시간만 실행 (월-금 9-18시): ~70% 절감

---

## 🔒 보안 권장사항

### 1. SSH 접근 제한

```hcl
# infra/dev/app/main.tf 수정
ingress_rules = [
  {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = "YOUR_IP/32"  # 특정 IP만 허용
    description = "SSH from my IP"
  }
]
```

### 2. Secrets 관리

```bash
# terraform.tfvars를 .gitignore에 추가 (이미 추가됨)
# 민감 정보는 AWS Secrets Manager 사용 권장
```

### 3. IAM 권한 최소화

```bash
# EC2 IAM Role에 필요한 최소 권한만 부여
# 현재: ECR Read + CloudWatch Logs
```

---

## 🐛 문제 해결

### 1. "Error: No valid credential sources found"

```bash
# AWS CLI 재설정
aws configure

# 자격 증명 확인
aws sts get-caller-identity
```

### 2. "Error: error configuring S3 Backend: no valid credential sources for S3 Backend found"

```bash
# Bootstrap을 먼저 apply 했는지 확인
cd infra/dev/bootstrap
terraform apply

# S3 버킷명을 dev/app/main.tf에 정확히 입력했는지 확인
```

### 3. SSH 접속 안 됨

```bash
# Security Group 확인
aws ec2 describe-security-groups --group-ids <SG_ID>

# SSH 키 권한 확인
chmod 600 ~/.ssh/shortlink-dev-key

# EC2 상태 확인
aws ec2 describe-instances --instance-ids <INSTANCE_ID>
```

---

## 📚 다음 단계

- [ ] Week 3-4: RDS, ElastiCache 추가
- [ ] Week 3-4: ALB + Auto Scaling Group
- [ ] Week 3-4: GitHub Actions CI/CD 연동
- [ ] Week 5-6: EKS 클러스터 구축
- [ ] Week 7-8: 무중단 배포 파이프라인

---

## 🔗 참고 자료

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Free Tier](https://aws.amazon.com/free/)
- [Terraform Best Practices](https://www.terraform-best-practices.com/)
