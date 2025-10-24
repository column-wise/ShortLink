#!/bin/bash
set -e

# Blue/Green Deployment Script for ShortLink
# This script performs zero-downtime deployment on a single EC2 instance

ECR_REGISTRY="${ECR_REGISTRY}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
COMPOSE_FILE="docker-compose.blue-green.yml"
NGINX_CONF="deploy/nginx/nginx.conf"

COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo -e "${COLOR_GREEN}ShortLink Blue/Green Deployment${COLOR_RESET}"
echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo "ECR Registry: $ECR_REGISTRY"
echo "Image Tag: $IMAGE_TAG"
echo ""

# Step 1: ECR Login
echo -e "${COLOR_YELLOW}[1/7] Logging in to ECR...${COLOR_RESET}"
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

# Step 2: Pull new images
echo -e "${COLOR_YELLOW}[2/7] Pulling new images...${COLOR_RESET}"
docker pull $ECR_REGISTRY/shortlink-api:$IMAGE_TAG
docker pull $ECR_REGISTRY/shortlink-events-consumer:$IMAGE_TAG

# Step 3: Check current environment (Blue or Green)
CURRENT_API_CONTAINER=$(docker ps --filter "name=shortlink-api" --format "{{.Names}}" | head -n 1)

if [ -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_BLUE}No existing containers found. Starting initial deployment...${COLOR_RESET}"
    DEPLOY_ENV="blue"
    CURRENT_PORT=8080
else
    if [[ "$CURRENT_API_CONTAINER" == *"blue"* ]]; then
        echo -e "${COLOR_BLUE}Current: BLUE, Deploying: GREEN${COLOR_RESET}"
        DEPLOY_ENV="green"
        CURRENT_PORT=8080
        NEW_PORT=8081
    else
        echo -e "${COLOR_GREEN}Current: GREEN, Deploying: BLUE${COLOR_RESET}"
        DEPLOY_ENV="blue"
        CURRENT_PORT=8081
        NEW_PORT=8080
    fi
fi

# Step 4: Start new environment containers
echo -e "${COLOR_YELLOW}[3/7] Starting $DEPLOY_ENV environment...${COLOR_RESET}"

if [ "$DEPLOY_ENV" = "blue" ]; then
    # Start Blue containers
    docker run -d \
        --name shortlink-api-blue \
        --network short-link_default \
        --env-file dev.env \
        -e ECR_REGISTRY=$ECR_REGISTRY \
        -e IMAGE_TAG=$IMAGE_TAG \
        -p ${NEW_PORT:-8080}:8080 \
        $ECR_REGISTRY/shortlink-api:$IMAGE_TAG

    docker run -d \
        --name shortlink-events-consumer-blue \
        --network short-link_default \
        --env-file dev.env \
        -e ECR_REGISTRY=$ECR_REGISTRY \
        -e IMAGE_TAG=$IMAGE_TAG \
        $ECR_REGISTRY/shortlink-events-consumer:$IMAGE_TAG
else
    # Start Green containers
    docker run -d \
        --name shortlink-api-green \
        --network short-link_default \
        --env-file dev.env \
        -e ECR_REGISTRY=$ECR_REGISTRY \
        -e IMAGE_TAG=$IMAGE_TAG \
        -p ${NEW_PORT}:8080 \
        $ECR_REGISTRY/shortlink-api:$IMAGE_TAG

    docker run -d \
        --name shortlink-events-consumer-green \
        --network short-link_default \
        --env-file dev.env \
        -e ECR_REGISTRY=$ECR_REGISTRY \
        -e IMAGE_TAG=$IMAGE_TAG \
        $ECR_REGISTRY/shortlink-events-consumer:$IMAGE_TAG
fi

# Step 5: Health check
echo -e "${COLOR_YELLOW}[4/7] Performing health check...${COLOR_RESET}"
sleep 10

for i in {1..30}; do
    if curl -f http://localhost:${NEW_PORT:-8080}/actuator/health > /dev/null 2>&1; then
        echo -e "${COLOR_GREEN}✓ Health check passed!${COLOR_RESET}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${COLOR_RED}✗ Health check failed after 30 attempts${COLOR_RESET}"
        echo "Rolling back..."
        docker stop shortlink-api-${DEPLOY_ENV} shortlink-events-consumer-${DEPLOY_ENV} || true
        docker rm shortlink-api-${DEPLOY_ENV} shortlink-events-consumer-${DEPLOY_ENV} || true
        exit 1
    fi
    echo "Waiting for health check... ($i/30)"
    sleep 2
done

# Step 6: Update Nginx configuration
echo -e "${COLOR_YELLOW}[5/7] Switching Nginx to $DEPLOY_ENV environment...${COLOR_RESET}"

if [ ! -z "$CURRENT_API_CONTAINER" ]; then
    # Update Nginx upstream to point to new port
    NEW_BACKEND="server api-server-${DEPLOY_ENV}:8080;"
    sed -i "s|server api-server.*|$NEW_BACKEND|g" $NGINX_CONF

    # Reload Nginx
    docker exec shortlink-nginx nginx -s reload
    echo -e "${COLOR_GREEN}✓ Nginx switched to $DEPLOY_ENV${COLOR_RESET}"

    sleep 5
fi

# Step 7: Stop old environment
if [ ! -z "$CURRENT_API_CONTAINER" ]; then
    echo -e "${COLOR_YELLOW}[6/7] Stopping old environment...${COLOR_RESET}"

    OLD_ENV=$([ "$DEPLOY_ENV" = "blue" ] && echo "green" || echo "blue")

    docker stop shortlink-api-${OLD_ENV} || true
    docker stop shortlink-events-consumer-${OLD_ENV} || true

    docker rm shortlink-api-${OLD_ENV} || true
    docker rm shortlink-events-consumer-${OLD_ENV} || true

    echo -e "${COLOR_GREEN}✓ Old environment stopped${COLOR_RESET}"
fi

# Step 8: Cleanup old images
echo -e "${COLOR_YELLOW}[7/7] Cleaning up old Docker images...${COLOR_RESET}"
docker image prune -f

echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
echo -e "${COLOR_GREEN}✓ Deployment completed successfully!${COLOR_RESET}"
echo -e "${COLOR_GREEN}Active environment: $DEPLOY_ENV${COLOR_RESET}"
echo -e "${COLOR_GREEN}========================================${COLOR_RESET}"
