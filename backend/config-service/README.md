# config-service

Spring Cloud Config Server for KRYPTO.

## Port

- `8888`

## Purpose

Serves centralized configuration to all config-client services.
This removes duplicated config and keeps environment-specific settings in one place.

## Config Source

- Local profile (`native`): `file:./config-repo`
- Docker profile: `file:/config-repo`

Source files live in [config-repo](../config-repo).

## Service Discovery

- Registers with Eureka (`discovery-service`)
- Other services import config via `spring.config.import`

## Health Endpoints

- `GET /actuator/health`
- `GET /actuator/info`

## Common Requests

```text
/{application}/{profile}
/{application}/{profile}/{label}
```

Examples:
- `/api-gateway/default`
- `/trading-service/docker`

## Run

```bash
./gradlew :config-service:bootRun
```
