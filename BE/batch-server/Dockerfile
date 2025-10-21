FROM gradle:8.10.2-jdk21-alpine AS build
WORKDIR /workspace

# 1) 루트 gradle 관련만 복사
COPY build.gradle gradlew ./
COPY gradle ./gradle

# 2) 멀티모듈 settings.gradle을 먼저 생성 (항상 최신 반영)
RUN printf "rootProject.name = 'short-link'\ninclude 'shared'\ninclude 'batch-server'\n" > settings.gradle

# 3) 모듈의 build.gradle만 먼저 복사 (의존성 캐시 분리)
COPY shared/build.gradle ./shared/build.gradle
COPY batch-server/build.gradle ./batch-server/build.gradle

# (선택) Gradle 캐시 사용 — BuildKit 필요. 없으면 RUN 줄만 남겨도 됨.
# RUN --mount=type=cache,target=/root/.gradle \
RUN chmod +x ./gradlew && ./gradlew :batch-server:dependencies --no-daemon

# 4) 소스 복사 (src가 바뀌어야만 이후 레이어가 무효화됨)
COPY shared/src ./shared/src
COPY batch-server/src ./batch-server/src

# 5) 빌드
# RUN --mount=type=cache,target=/root/.gradle \
RUN ./gradlew :batch-server:bootJar --no-daemon

# ---------- Runtime Image ----------
FROM eclipse-temurin:21-jre-alpine
ENV SPRING_PROFILES_ACTIVE=dev
WORKDIR /app
COPY --from=build /workspace/batch-server/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
