#!/bin/bash
set -e

# 로그 파일
exec > >(tee /var/log/user-data.log)
exec 2>&1

echo "========================================"
echo "ShortLink Dev Server Setup Starting..."
echo "========================================"

# 시스템 업데이트
echo "[1/6] Updating system packages..."
dnf update -y

# Docker 설치
echo "[2/6] Installing Docker..."
dnf install -y docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Docker Compose 설치
echo "[3/6] Installing Docker Compose..."
DOCKER_COMPOSE_VERSION="2.24.5"
curl -L "https://github.com/docker/compose/releases/download/v$${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

# AWS CLI 설치 (Amazon Linux 2023에는 기본 포함)
echo "[4/6] Verifying AWS CLI..."
aws --version

# ECR 로그인 스크립트 생성
echo "[5/6] Creating ECR login script..."
cat > /usr/local/bin/ecr-login.sh << 'EOF'
#!/bin/bash
aws ecr get-login-password --region ${aws_region} | docker login --username AWS --password-stdin ${ecr_repository_api}
EOF
chmod +x /usr/local/bin/ecr-login.sh

# 애플리케이션 디렉토리 생성
echo "[6/6] Creating application directory..."
mkdir -p /opt/shortlink
chown -R ec2-user:ec2-user /opt/shortlink

# 환경 변수 파일 템플릿 생성
cat > /opt/shortlink/.env.template << 'EOF'
# MySQL
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=shortlink
MYSQL_USER=shortlink
MYSQL_PASSWORD=shortlink
MYSQL_HOST=mysql
MYSQL_PORT=3306

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
APP_KAFKA_TOPICS_LINKHITS=link_hits

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
SERVER_URL=http://localhost:8080
EOF

echo "========================================"
echo "ShortLink Dev Server Setup Completed!"
echo "========================================"
echo "Docker version: $(docker --version)"
echo "Docker Compose version: $(docker-compose --version)"
echo "AWS CLI version: $(aws --version)"
echo ""
echo "Next steps:"
echo "1. SSH to server: ssh -i ~/.ssh/shortlink-dev-key ec2-user@<PUBLIC_IP>"
echo "2. Clone repository"
echo "3. Run: /usr/local/bin/ecr-login.sh"
echo "4. Deploy with docker-compose"
