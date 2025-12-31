# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM from build context root
COPY ../../pom.xml .
# Copy this service's POM
COPY pom.xml discovery/

# Create empty directories for other modules (to satisfy Maven)
RUN mkdir -p gateway common test-service auth-service config

# Download dependencies
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -pl discovery -am || \
    mvn dependency:go-offline -N

# Copy source code
COPY src discovery/src/

# Build
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -pl discovery -am -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/discovery/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]