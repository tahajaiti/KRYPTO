# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS builder

ARG SERVICE_NAME

WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
COPY common/build.gradle.kts common/
COPY discovery-service/build.gradle.kts discovery-service/
COPY config-service/build.gradle.kts config-service/
COPY api-gateway/build.gradle.kts api-gateway/
COPY user-service/build.gradle.kts user-service/
COPY wallet-service/build.gradle.kts wallet-service/
COPY coin-service/build.gradle.kts coin-service/
COPY trading-service/build.gradle.kts trading-service/
COPY blockchain-service/build.gradle.kts blockchain-service/
COPY gamification-service/build.gradle.kts gamification-service/

RUN chmod +x gradlew

# prime dependency cache using only build scripts for better layer reuse
RUN --mount=type=cache,target=/root/.gradle \
	./gradlew :${SERVICE_NAME}:dependencies -x test --no-daemon

COPY common/src common/src
COPY discovery-service/src discovery-service/src
COPY config-service/src config-service/src
COPY api-gateway/src api-gateway/src
COPY user-service/src user-service/src
COPY wallet-service/src wallet-service/src
COPY coin-service/src coin-service/src
COPY trading-service/src trading-service/src
COPY blockchain-service/src blockchain-service/src
COPY gamification-service/src gamification-service/src

RUN --mount=type=cache,target=/root/.gradle \
	./gradlew :${SERVICE_NAME}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

ARG SERVICE_NAME

WORKDIR /app

COPY --from=builder /app/${SERVICE_NAME}/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
