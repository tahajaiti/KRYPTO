# config-repo

Centralized Spring Cloud Config repository for KRYPTO.

This folder contains externalized configuration for all services and profiles.
`config-service` reads these files and serves them to clients at startup.

## File Conventions

- `application.yml`: shared defaults across services
- `application-docker.yml`: shared Docker overrides
- `<service>.yml`: service-specific defaults
- `<service>-docker.yml`: service-specific Docker overrides

Examples:
- `user-service.yml`
- `trading-service.yml`
- `gamification-service.yml`

## Profiles

- `default`: local development (localhost hosts)
- `docker`: container network hosts (`postgres`, `rabbitmq`, `redis`, etc.)

## How It Is Used

1. Service starts and loads local `spring.application.name`
2. Service imports config from `config-service`
3. `config-service` resolves file by service name and active profile

Example resolution:
- `api-gateway` + default -> `api-gateway.yml`
- `api-gateway` + docker -> `api-gateway.yml` + `api-gateway-docker.yml`

## Best Practices

- Keep secrets in env vars (`${VAR:default}` style)
- Prefer service-specific values in service files
- Keep cross-cutting defaults in `application.yml`
- Avoid duplicating same keys across many files unless profile-specific
