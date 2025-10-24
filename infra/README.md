# ShortLink Infrastructure as Code

Terraform?¼ë¡œ ê´€ë¦¬í•˜??ShortLink ?„ë¡œ?íŠ¸ ?¸í”„??
## ?“ êµ¬ì¡°

```
infra/
?œâ??€ dev/                  # ê°œë°œ ?˜ê²½
??  ?œâ??€ bootstrap/        # ê¸°ë°˜ ?¸í”„??(S3, VPC, ECR)
??  ?”â??€ app/              # ? í”Œë¦¬ì??´ì…˜ ?¸í”„??(EC2)
???œâ??€ prod/                 # ?„ë¡œ?•ì…˜ ?˜ê²½
??  ?œâ??€ bootstrap/
??  ?”â??€ app/
???”â??€ modules/              # ?¬ì‚¬??ê°€?¥í•œ ëª¨ë“ˆ
    ?œâ??€ vpc/
    ?œâ??€ ec2/
    ?œâ??€ security-group/
    ?œâ??€ ecr/
    ?œâ??€ rds/
    ?œâ??€ elasticache/
    ?”â??€ alb/
```

## ?? ?œì‘?˜ê¸°

### ?¬ì „ ì¤€ë¹?
1. **AWS CLI ?¤ì¹˜ ë°??¤ì •**
```bash
# AWS CLI ?¤ì¹˜ (macOS)
brew install awscli

# AWS ?ê²© ì¦ëª… ?¤ì •
aws configure
# AWS Access Key ID: [your-access-key]
# AWS Secret Access Key: [your-secret-key]
# Default region name: ap-northeast-2
# Default output format: json

# ê³„ì • ID ?•ì¸
aws sts get-caller-identity --query Account --output text
```

2. **Terraform ?¤ì¹˜**
```bash
# macOS
brew install terraform

# ë²„ì „ ?•ì¸ (1.5 ?´ìƒ ?„ìš”)
terraform version
```

3. **SSH ???ì„±**
```bash
# EC2 ?‘ì†??SSH ???ì„±
ssh-keygen -t rsa -b 4096 -f ~/.ssh/shortlink-dev-key

# ê³µê°œ???•ì¸ (??ê°’ì„ terraform.tfvars???…ë ¥)
cat ~/.ssh/shortlink-dev-key.pub
```

---

## ?“ ë°°í¬ ?œì„œ

### Step 1: Bootstrap ?¸í”„??ë°°í¬

Bootstrap?€ **??ë²ˆë§Œ ?¤í–‰**?˜ì—¬ ê¸°ë°˜ ?¸í”„?¼ë? ?ì„±?©ë‹ˆ??

```bash
cd infra/dev/bootstrap

# 1. terraform.tfvars ?˜ì •
# - aws_account_id: ?¤ì œ AWS ê³„ì • ID
vi terraform.tfvars

# 2. Terraform ì´ˆê¸°??terraform init -backend-config="bucket=<STATE_BUCKET_NAME>" -backend-config="key=dev/bootstrap/terraform.tfstate" -backend-config="region=ap-northeast-2" -backend-config="dynamodb_table=shortlink-terraform-lock"`r`n
# 3. ?¤í–‰ ê³„íš ?•ì¸
terraform plan

# 4. ?¸í”„???ì„±
terraform apply

# 5. Output ?•ì¸
terraform output
```

**?ì„±?˜ëŠ” ë¦¬ì†Œ??**
- S3 ë²„í‚· (Terraform ?íƒœ ?€??
- DynamoDB ?Œì´ë¸?(?íƒœ ??
- VPC, ?œë¸Œ?? Internet Gateway, NAT Gateway
- ECR ë¦¬í¬ì§€? ë¦¬ (API, Events Consumer)

**ì¤‘ìš”:** Bootstrap apply ??ì¶œë ¥?˜ëŠ” S3 ë²„í‚·ëª…ì„ ë³µì‚¬?´ì„œ `dev/app/main.tf`??backend ?¤ì •???…ë ¥?˜ì„¸??

---

### Step 2: Application ?¸í”„??ë°°í¬

```bash
cd infra/dev/app

# 1. terraform.tfvars ?˜ì •
# - ssh_public_key: cat ~/.ssh/shortlink-dev-key.pub ??ì¶œë ¥ê°?vi terraform.tfvars

# 2. main.tf??backend ?¤ì • ?˜ì •
# - bucket: bootstrap output??s3_bucket_name ê°?vi main.tf

# 3. Terraform ì´ˆê¸°??(S3 ë°±ì—”???¬ìš©)
terraform init -backend-config="bucket=<STATE_BUCKET_NAME>" -backend-config="key=dev/bootstrap/terraform.tfstate" -backend-config="region=ap-northeast-2" -backend-config="dynamodb_table=shortlink-terraform-lock"`r`n
# 4. ?¤í–‰ ê³„íš ?•ì¸
terraform plan

# 5. ?¸í”„???ì„±
terraform apply

# 6. Output ?•ì¸ (Public IP ?•ì¸)
terraform output
```

**?ì„±?˜ëŠ” ë¦¬ì†Œ??**
- EC2 ?¸ìŠ¤?´ìŠ¤ (t2.micro)
- Security Group (SSH, HTTP, HTTPS)
- IAM Role (ECR ?‘ê·¼ ê¶Œí•œ)
- Key Pair

---

## ?–¥ï¸?EC2 ?‘ì† ë°?? í”Œë¦¬ì??´ì…˜ ë°°í¬

### 1. SSH ?‘ì†

```bash
# Public IP ?•ì¸
cd infra/dev/app
terraform output instance_public_ip

# SSH ?‘ì†
ssh -i ~/.ssh/shortlink-dev-key ec2-user@<PUBLIC_IP>
```

### 2. ? í”Œë¦¬ì??´ì…˜ ë°°í¬

```bash
# EC2 ?œë²„ ?´ë??ì„œ ?¤í–‰

# 1. ?„ë¡œ?íŠ¸ ?´ë¡ 
cd /opt/shortlink
git clone <YOUR_REPOSITORY_URL>
cd short-link/BE

# 2. ECR ë¡œê·¸??/usr/local/bin/ecr-login.sh

# 3. ?˜ê²½ ë³€???Œì¼ ?ì„±
cp /opt/shortlink/.env.template .env

# 4. Docker Composeë¡??„ì²´ ?¤íƒ ?¤í–‰
docker-compose up -d

# 5. ë¡œê·¸ ?•ì¸
docker-compose logs -f api-server
```

### 3. ?¬ìŠ¤ì²´í¬

```bash
# ë¡œì»¬?ì„œ ?¤í–‰
PUBLIC_IP=$(cd infra/dev/app && terraform output -raw instance_public_ip)

curl http://$PUBLIC_IP:8080/actuator/health
# ?‘ë‹µ: {"status":"UP"}
```

---

## ?”§ ì£¼ìš” ëª…ë ¹??
### Terraform

```bash
# ë³€ê²??¬í•­ ?•ì¸
terraform plan

# ?¸í”„???ìš©
terraform apply

# ?¹ì • ë¦¬ì†Œ?¤ë§Œ ?ìš©
terraform apply -target=module.app_server

# ë¦¬ì†Œ???? œ
terraform destroy

# Output ?•ì¸
terraform output

# ?íƒœ ?•ì¸
terraform state list
terraform state show <RESOURCE>
```

### AWS CLI

```bash
# ECR ë¦¬í¬ì§€? ë¦¬ ëª©ë¡
aws ecr describe-repositories

# ECR ?´ë?ì§€ ëª©ë¡
aws ecr list-images --repository-name shortlink-api

# EC2 ?¸ìŠ¤?´ìŠ¤ ëª©ë¡
aws ec2 describe-instances --filters "Name=tag:Project,Values=shortlink"

# VPC ëª©ë¡
aws ec2 describe-vpcs --filters "Name=tag:Project,Values=shortlink"
```

---

## ?—‘ï¸?ë¦¬ì†Œ???? œ

**ì£¼ì˜:** ?? œ ?œì„œë¥?ì§€ì¼œì•¼ ?©ë‹ˆ??

```bash
# 1. App ?¸í”„???? œ
cd infra/dev/app
terraform destroy

# 2. Bootstrap ?¸í”„???? œ
cd ../bootstrap
terraform destroy
```

**ì°¸ê³ :** S3 ë²„í‚·ê³?DynamoDB ?Œì´ë¸”ì? `prevent_destroy = true`ë¡?ë³´í˜¸?˜ì–´ ?ˆìŠµ?ˆë‹¤. ?? œ?˜ë ¤ë©?main.tf?ì„œ ?´ë‹¹ ?¤ì •???œê±°?´ì•¼ ?©ë‹ˆ??

---

## ?“Š ë¹„ìš© ?ˆìƒ

### \xC9\xB4\xEC\xA0\x95 \xEC\x84\xA4\xEC\xA0\x95 (ap-northeast-2)

| ë¦¬ì†Œ??| ?¬ì–‘ | ??ë¹„ìš© |
|--------|------|---------|
| EC2 | t2.micro (750?œê°„ Free Tier) | **$0** |
| EBS | 30GB gp3 | $2.40 |
| NAT Gateway | 1ê°?| $32.40 |
| Data Transfer | 100GB ?´ë‚´ | **$0** |
| S3 | Terraform ?íƒœ (< 1GB) | $0.02 |
| **ì´ê³„** | | **~$35/??* |

**ë¹„ìš© ?ˆê° ??**
- ?¬ìš©?˜ì? ?Šì„ ??EC2 ì¤‘ì?: `aws ec2 stop-instances --instance-ids <INSTANCE_ID>`
- NAT Gateway ?œê±° (Public Subnetë§??¬ìš©): $32 ?ˆê°
- ?…ë¬´ ?œê°„ë§??¤í–‰ (??ê¸?9-18??: ~70% ?ˆê°

---

## ?”’ ë³´ì•ˆ ê¶Œì¥?¬í•­

### 1. SSH ?‘ê·¼ ?œí•œ

```hcl
# infra/dev/app/main.tf ?˜ì •
ingress_rules = [
  {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = "YOUR_IP/32"  # ?¹ì • IPë§??ˆìš©
    description = "SSH from my IP"
  }
]
```

### 2. Secrets ê´€ë¦?
```bash
# terraform.tfvarsë¥?.gitignore??ì¶”ê? (?´ë? ì¶”ê???
# ë¯¼ê° ?•ë³´??AWS Secrets Manager ?¬ìš© ê¶Œì¥
```

### 3. IAM ê¶Œí•œ ìµœì†Œ??
```bash
# EC2 IAM Role???„ìš”??ìµœì†Œ ê¶Œí•œë§?ë¶€??# ?„ì¬: ECR Read + CloudWatch Logs
```

---

## ?› ë¬¸ì œ ?´ê²°

### 1. "Error: No valid credential sources found"

```bash
# AWS CLI ?¬ì„¤??aws configure

# ?ê²© ì¦ëª… ?•ì¸
aws sts get-caller-identity
```

### 2. "Error: error configuring S3 Backend: no valid credential sources for S3 Backend found"

```bash
# Bootstrap??ë¨¼ì? apply ?ˆëŠ”ì§€ ?•ì¸
cd infra/dev/bootstrap
terraform apply

# S3 ë²„í‚·ëª…ì„ dev/app/main.tf???•í™•???…ë ¥?ˆëŠ”ì§€ ?•ì¸
```

### 3. SSH ?‘ì† ????
```bash
# Security Group ?•ì¸
aws ec2 describe-security-groups --group-ids <SG_ID>

# SSH ??ê¶Œí•œ ?•ì¸
chmod 600 ~/.ssh/shortlink-dev-key

# EC2 ?íƒœ ?•ì¸
aws ec2 describe-instances --instance-ids <INSTANCE_ID>
```

---

## ?“š ?¤ìŒ ?¨ê³„

- [ ] Week 3-4: RDS, ElastiCache ì¶”ê?
- [ ] Week 3-4: ALB + Auto Scaling Group
- [ ] Week 3-4: GitHub Actions CI/CD ?°ë™
- [ ] Week 5-6: EKS ?´ëŸ¬?¤í„° êµ¬ì¶•
- [ ] Week 7-8: ë¬´ì¤‘??ë°°í¬ ?Œì´?„ë¼??
---

## ?”— ì°¸ê³  ?ë£Œ

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Free Tier](https://aws.amazon.com/free/)
- [Terraform Best Practices](https://www.terraform-best-practices.com/)


