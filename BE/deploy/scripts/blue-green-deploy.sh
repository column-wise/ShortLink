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
echo -e "${COLOR_YELLOW}[1/7] Logging in to ECR...${COLOR_RESET}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# Step 2: Pull new images
echo -e "${COLOR_YELLOW}[2/7] Pulling new images...${COLOR_RESET}"
docker pull "${ECR_REGISTRY}/shortlink-api:${IMAGE_TAG}"
docker pull "${ECR_REGISTRY}/shortlink-events-consumer:${IMAGE_TAG}"
# Curl image for in-network health checks
docker pull curlimages/curl:8.10.1

# Step 3: Ensure Docker network exists
echo -e "${COLOR_YELLOW}[3/7] Ensuring Docker network exists...${COLOR_RESET}"
docker network inspect short-link_default >/dev/null 2>&1 || docker network create short-link_default
echo -e "${COLOR_GREEN}✓ Network ready${COLOR_RESET}"

# Step 4: Start infrastructure containers (MySQL, Redis, Kafka)
echo -e "${COLOR_YELLOW}[4/7] Starting infrastructure containers...${COLOR_RESET}"
docker-compose -f docker-compose.blue-green.yml up -d mysql redis kafka nginx

echo -e "${COLOR_BLUE}Waiting for infrastructure services to be ready...${COLOR_RESET}"
sleep 15

echo -e "${COLOR_GREEN}✓ Infrastructure containers started${COLOR_RESET}"

# Step 5: Check current environment
CURRENT_API_CONTAINER=$(docker ps --filter "name=shortlink-api" --filter "status=running" --format "{{.Names}}" | head -n 1)

if [ -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_BLUE}No existing containers found. Starting initial deployment...${COLOR_RESET}"
    DEPLOY_SUFFIX=""
else
    echo -e "${COLOR_BLUE}Current container: ${CURRENT_API_CONTAINER}${COLOR_RESET}"
    DEPLOY_SUFFIX="-green"
fi

# Step 6: Start new environment containers
echo -e "${COLOR_YELLOW}[5/7] Starting new environment...${COLOR_RESET}"

docker run -d \
    --name "shortlink-api${DEPLOY_SUFFIX}" \
    --network short-link_default \
    --env-file dev.env \
    "${ECR_REGISTRY}/shortlink-api:${IMAGE_TAG}"

docker run -d \
    --name "shortlink-events-consumer${DEPLOY_SUFFIX}" \
    --network short-link_default \
    --env-file dev.env \
    "${ECR_REGISTRY}/shortlink-events-consumer:${IMAGE_TAG}"

echo -e "${COLOR_GREEN}✓ New containers started${COLOR_RESET}"

# Step 7: Health check (API + Events Consumer)
echo -e "${COLOR_YELLOW}[6/7] Performing health checks...${COLOR_RESET}"
sleep 10

# Target the new environment directly over the Docker network
TARGET_NAME="shortlink-api${DEPLOY_SUFFIX}"
for i in {1..30}; do
    if docker run --rm --network short-link_default curlimages/curl:8.10.1 -fsS "http://${TARGET_NAME}:8080/actuator/health" > /dev/null 2>&1; then
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

# Events Consumer readiness via Docker health status
CONSUMER_NAME="shortlink-events-consumer${DEPLOY_SUFFIX}"
for i in {1..30}; do
    if docker ps --format '{{.Names}}' | grep -q "^${CONSUMER_NAME}$"; then
        STATUS=$(docker inspect -f '{{.State.Health.Status}}' "${CONSUMER_NAME}" 2>/dev/null || echo "unknown")
        if [ "${STATUS}" = "healthy" ]; then
            echo -e "${COLOR_GREEN}✓ Events consumer healthy${COLOR_RESET}"
            break
        fi
    fi
    if [ $i -eq 30 ]; then
        echo -e "${COLOR_RED}✗ Events consumer failed to become healthy${COLOR_RESET}"
        echo "Rolling back..."
        docker stop "shortlink-api${DEPLOY_SUFFIX}" "shortlink-events-consumer${DEPLOY_SUFFIX}" || true
        docker rm "shortlink-api${DEPLOY_SUFFIX}" "shortlink-events-consumer${DEPLOY_SUFFIX}" || true
        exit 1
    fi
    echo "Waiting for events consumer health... ($i/30)"
    sleep 2
done

# Step 8: Stop old environment and rename new one
if [ ! -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_YELLOW}[7/7] Switching to new environment...${COLOR_RESET}"

    # Stop old containers
    docker stop shortlink-api shortlink-events-consumer || true
    docker rm shortlink-api shortlink-events-consumer || true

    # Rename new containers to become active
    docker rename shortlink-api-green shortlink-api || true
    docker rename shortlink-events-consumer-green shortlink-events-consumer || true

    echo -e "${COLOR_GREEN}✓ Environment switched${COLOR_RESET}"
fi

# Reload Nginx to re-resolve upstream target
if docker ps --format '{{.Names}}' | grep -q '^shortlink-nginx$'; then
    echo -e "${COLOR_YELLOW}Reloading Nginx...${COLOR_RESET}"
    docker exec shortlink-nginx nginx -s reload || docker restart shortlink-nginx || true
    echo -e "${COLOR_GREEN}✓ Nginx reloaded${COLOR_RESET}"
fi

# Cleanup old images
echo -e "${COLOR_YELLOW}Cleaning up old Docker images...${COLOR_RESET}"
docker image prune -f

echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo -e "${COLOR_GREEN}✓ Deployment completed successfully!${COLOR_RESET}"
echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
