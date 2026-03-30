# api-gateway

Spring Cloud Gateway service for KRYPTO.

## Port

- `8080`

## Purpose

- Single public entry point for frontend and external clients
- Route requests to internal services
- Apply cross-cutting concerns (for example CORS at gateway boundary)

## Routes

Configured in `config-repo/api-gateway.yml`:

- `/api/auth/**`, `/api/users/**` -> `user-service`
- `/api/wallets/**` -> `wallet-service`
- `/api/coins/**` -> `coin-service`
- `/api/trades/**`, `/api/orders/**` -> `trading-service`
- `/api/blockchain/**` -> `blockchain-service`

## CORS

Global CORS is configured in `config-repo/api-gateway.yml` under:

- `spring.cloud.gateway.server.webflux.globalcors`

Frontend origin is controlled by env var:

- `FRONTEND_ORIGIN` (default: `http://localhost:4200`)

Credentials are enabled so cookie-based auth can work through the gateway.

## Actuator

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/gateway/**` (read-only)

## Run

```bash
./gradlew :api-gateway:bootRun
```
