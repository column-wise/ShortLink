# ShortLink Infrastructure as Code

Terraform로 AWS 개발 환경을 코드로 관리합니다.

## 디렉터리 구조

```
infra/
└── dev/                  # 개발 환경
    ├── bootstrap/        # 기반 인프라(S3, VPC, ECR)
    └── app/              # 애플리케이션 인프라(EC2)
└── modules/              # 재사용 가능한 모듈
    ├── vpc/
    ├── ec2/
    └── security-group/
```

## 시작하기

### 사전 준비
1. AWS CLI 설치 및 설정
```bash
brew install awscli
aws configure
# Default region name: ap-northeast-2
```

2. Terraform 설치 (1.5 이상 권장)
```bash
brew install terraform
terraform version
```

3. SSH 키 생성 (EC2 접속용)
```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/shortlink-dev-key
cat ~/.ssh/shortlink-dev-key.pub
```

---

## 배포 절차

### 1) Bootstrap 스택 배포 (S3, DynamoDB, VPC, ECR)
```bash
cd infra/dev/bootstrap

# 원격 상태 백엔드 초기화 (예시)
terraform init \
  -backend-config="bucket=<STATE_BUCKET_NAME>" \
  -backend-config="key=dev/bootstrap/terraform.tfstate" \
  -backend-config="region=ap-northeast-2" \
  -backend-config="dynamodb_table=shortlink-terraform-lock"

terraform plan
terraform apply
terraform output
```

생성 리소스
- S3 버킷 (Terraform 상태)
- DynamoDB 테이블 (상태 락)
- VPC, 서브넷, IGW, NAT Gateway
- ECR 리포지토리 (api, events-consumer)

중요: 출력된 S3 버킷명을 infra/dev/app에서 사용합니다.

---

### 2) App 스택 배포 (EC2)
```bash
cd ../app

# 입력 변수
# - var.state_bucket_name: Bootstrap에서 만든 상태 버킷명
# - var.ssh_public_key: ~/.ssh/shortlink-dev-key.pub 내용

terraform init
terraform plan
terraform apply

terraform output  # public IP, SSH 명령 확인
```

생성 리소스
- EC2 인스턴스 (t2.micro)
- Security Group (22, 80, 443, 8080)
- IAM Role/Instance Profile (ECR, CloudWatch Logs 권한)
- Key Pair

---

## EC2 접속 및 애플리케이션 배포 (예시)
```bash
# Public IP 확인
cd infra/dev/app && terraform output -raw instance_public_ip

# SSH 접속
ssh -i ~/.ssh/shortlink-dev-key ec2-user@<PUBLIC_IP>

# (선택) 서버에서 저장소 클론
# git clone <YOUR_REPOSITORY_URL>
```

---

## 주요 명령어
```bash
terraform plan
terraform apply
terraform destroy
```

## 보안/운영 팁
- SSH 접근은 고정 IP(예: GitHub Actions)로 제한 권장
- terraform.tfvars는 .gitignore에 포함하고 비밀은 Secrets Manager 사용 권장
- IAM 권한은 최소 권한 원칙 준수 (현재: ECR Read, CloudWatch Logs)
