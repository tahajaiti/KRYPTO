# discovery-service

Eureka Server for KRYPTO service registration and discovery.

## Port

- `8761`

## Purpose

- Acts as service registry for all microservices
- Enables client-side discovery for gateway and inter-service communication

## Configuration Highlights

- `register-with-eureka: false`
- `fetch-registry: false`

This service is the registry and does not register itself as a client.

## Dashboard

- Eureka UI: `http://localhost:8761`

## Health Endpoints

- `GET /actuator/health`
- `GET /actuator/info`

## Run

```bash
./gradlew :discovery-service:bootRun
```

## Boot Order Note

Start this service first, then `config-service`, then the remaining services.
