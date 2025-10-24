#!/bin/bash
set -e

# Blue/Green Deployment Script for ShortLink
# This script performs zero-downtime deployment on a single EC2 instance

ECR_REGISTRY="${ECR_REGISTRY}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
NGINX_CONF="deploy/nginx/nginx.conf"

COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo -e "${COLOR_GREEN}ShortLink Blue/Green Deployment${COLOR_RESET}"
echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo "ECR Registry: ${ECR_REGISTRY}"
echo "Image Tag: ${IMAGE_TAG}"
echo ""

# Step 1: ECR Login
echo -e "${COLOR_YELLOW}[1/6] Logging in to ECR...${COLOR_RESET}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# Step 2: Pull new images
echo -e "${COLOR_YELLOW}[2/6] Pulling new images...${COLOR_RESET}"
docker pull "${ECR_REGISTRY}/shortlink-api:${IMAGE_TAG}"
docker pull "${ECR_REGISTRY}/shortlink-events-consumer:${IMAGE_TAG}"

# Step 3: Ensure Docker network exists
echo -e "${COLOR_YELLOW}[3/6] Ensuring Docker network exists...${COLOR_RESET}"
docker network inspect short-link_default >/dev/null 2>&1 || docker network create short-link_default
echo -e "${COLOR_GREEN}✓ Network ready${COLOR_RESET}"

# Step 4: Check current environment
CURRENT_API_CONTAINER=$(docker ps --filter "name=shortlink-api" --filter "status=running" --format "{{.Names}}" | head -n 1)

if [ -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_BLUE}No existing containers found. Starting initial deployment...${COLOR_RESET}"
    DEPLOY_SUFFIX=""
else
    echo -e "${COLOR_BLUE}Current container: ${CURRENT_API_CONTAINER}${COLOR_RESET}"
    DEPLOY_SUFFIX="-green"
fi

# Step 5: Start new environment containers
echo -e "${COLOR_YELLOW}[4/6] Starting new environment...${COLOR_RESET}"

docker run -d \
    --name "shortlink-api${DEPLOY_SUFFIX}" \
    --network short-link_default \
    --env-file dev.env \
    -p 8080:8080 \
    "${ECR_REGISTRY}/shortlink-api:${IMAGE_TAG}"

docker run -d \
    --name "shortlink-events-consumer${DEPLOY_SUFFIX}" \
    --network short-link_default \
    --env-file dev.env \
    "${ECR_REGISTRY}/shortlink-events-consumer:${IMAGE_TAG}"

echo -e "${COLOR_GREEN}✓ New containers started${COLOR_RESET}"

# Step 6: Health check
echo -e "${COLOR_YELLOW}[5/6] Performing health check...${COLOR_RESET}"
sleep 10

for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo -e "${COLOR_GREEN}✓ Health check passed!${COLOR_RESET}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${COLOR_RED}✗ Health check failed after 30 attempts${COLOR_RESET}"
        echo "Rolling back..."
        docker stop "shortlink-api${DEPLOY_SUFFIX}" "shortlink-events-consumer${DEPLOY_SUFFIX}" || true
        docker rm "shortlink-api${DEPLOY_SUFFIX}" "shortlink-events-consumer${DEPLOY_SUFFIX}" || true
        exit 1
    fi
    echo "Waiting for health check... ($i/30)"
    sleep 2
done

# Step 7: Stop old environment and rename new one
if [ ! -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_YELLOW}[6/7] Switching to new environment...${COLOR_RESET}"

    # Stop old containers
    docker stop shortlink-api shortlink-events-consumer || true
    docker rm shortlink-api shortlink-events-consumer || true

    # Rename new containers to become active
    docker rename shortlink-api-green shortlink-api || true
    docker rename shortlink-events-consumer-green shortlink-events-consumer || true

    echo -e "${COLOR_GREEN}✓ Environment switched${COLOR_RESET}"
fi

# Step 8: Cleanup old images
echo -e "${COLOR_YELLOW}[7/7] Cleaning up old Docker images...${COLOR_RESET}"
docker image prune -f

echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo -e "${COLOR_GREEN}✓ Deployment completed successfully!${COLOR_RESET}"
echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
