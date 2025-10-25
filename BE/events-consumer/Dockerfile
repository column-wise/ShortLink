# syntax=docker/dockerfile:1
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace

# 1) 루트 gradle 구성만 복사
COPY build.gradle gradlew ./
COPY gradle ./gradle

# 2) 멀티모듈 settings.gradle 최소 구성
RUN printf "rootProject.name = 'short-link'\ninclude 'events-consumer'\n" > settings.gradle

# 3) 모듈의 build.gradle 먼저 복사
COPY events-consumer/build.gradle ./events-consumer/build.gradle

# 4) 의존성만 미리 캐시
RUN chmod +x ./gradlew && ./gradlew :events-consumer:dependencies --no-daemon

# 5) 소스 복사
COPY events-consumer/src ./events-consumer/src

# 6) 빌드 (bootJar 생성)
RUN ./gradlew :events-consumer:bootJar --no-daemon

# ---------- Runtime Image ----------
FROM eclipse-temurin:21-jre-alpine
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /app
COPY --from=build /workspace/events-consumer/build/libs/*.jar app.jar

# Ensure log directory exists and write app logs there for health probes
RUN mkdir -p /app/logs

# Docker healthcheck without exposing HTTP: mark healthy once app has started
# This checks for the Spring Boot startup message in the log file
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=12 \
  CMD sh -c "grep -q 'Started EventsConsumerApplication' /app/logs/spring.log || exit 1"

ENTRYPOINT ["java","-Dlogging.file.path=/app/logs","-jar","app.jar"]
