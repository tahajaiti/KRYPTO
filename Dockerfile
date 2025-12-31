# syntax=docker/dockerfile:labs

FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

ARG SERVICE_NAME

COPY pom.xml .

COPY --parents */pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl ${SERVICE_NAME} -am -q

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl ${SERVICE_NAME} -am -DskipTests -q


FROM eclipse-temurin:21-jre-alpine

# adding curl for healthcheks
RUN apk add --no-cache curl

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

ARG SERVICE_NAME

COPY --from=builder --chown=appuser:appgroup /workspace/${SERVICE_NAME}/target/*.jar app.jar

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]