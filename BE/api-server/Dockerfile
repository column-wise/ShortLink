# syntax=docker/dockerfile:1
FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace

# 1) 루트 gradle 관련만 복사
COPY build.gradle gradlew ./
COPY gradle ./gradle

# 2) 멀티모듈 settings.gradle 먼저 생성
RUN printf "rootProject.name = 'short-link'\ninclude 'shared'\ninclude 'api-server'\n" > settings.gradle

# 3) 모듈의 build.gradle만 먼저 복사
COPY shared/build.gradle ./shared/build.gradle
COPY api-server/build.gradle ./api-server/build.gradle

# 4) 의존성만 프리페치
# RUN --mount=type=cache,target=/root/.gradle \
RUN chmod +x ./gradlew && ./gradlew :api-server:dependencies --no-daemon

# 5) 소스 복사
COPY shared/src ./shared/src
COPY api-server/src ./api-server/src

# 6) 빌드
# RUN --mount=type=cache,target=/root/.gradle \
RUN ./gradlew :api-server:bootJar --no-daemon

# ---------- Runtime Image ----------
FROM eclipse-temurin:21-jre-alpine
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /app
COPY --from=build /workspace/api-server/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
