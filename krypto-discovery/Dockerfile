FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

ARG GITHUB_USERNAME
ARG GITHUB_TOKEN
ARG SERVICE_NAME

COPY . .

RUN mvn clean package -pl ${SERVICE_NAME} -am -s settings.xml -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG SERVICE_NAME

COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]