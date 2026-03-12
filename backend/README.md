# KRYPTO

KRYPTO is a gamified virtual cryptocurrency platform built with Spring Boot microservices.
Users receive KRYP (base currency), create custom coins, trade them, and progress through challenges and badges.

## Architecture

- Java 21, Spring Boot 4.0.3, Spring Cloud 2025.1.1
- Service discovery via Eureka
- Centralized configuration via Spring Cloud Config (`config-repo`)
- API routing via Spring Cloud Gateway
- Async messaging via RabbitMQ
- Caching via Redis
- PostgreSQL databases (one per business service)
- Docker Compose for local orchestration

## Services

| Service | Port | Purpose |
|---|---:|---|
| discovery-service | 8761 | Eureka service registry |
| config-service | 8888 | Centralized configuration server |
| api-gateway | 8080 | Single entry point and route gateway |
| user-service | 8081 | Auth, profile, user management |
| wallet-service | 8082 | Wallet balances, transfers, net worth |
| coin-service | 8083 | Coin creation, listing, pricing |
| trading-service | 8084 | Matching engine and trade execution |
| blockchain-service | 8085 | Custom blockchain transaction ledger |
| gamification-service | 8086 | Challenges, badges, leaderboard |

## Project Structure

```text
KRYPTO/
├── common/
├── discovery-service/
├── config-service/
├── api-gateway/
├── user-service/
├── wallet-service/
├── coin-service/
├── trading-service/
├── blockchain-service/
├── gamification-service/
├── config-repo/
├── infrastructure/
├── docker-compose.yml
├── build.gradle.kts
└── settings.gradle.kts
```

## Environment Variables

Main variables are documented in `.env.example`.

Key values:
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
- `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`
- `REDIS_HOST`, `REDIS_PORT`
- `JWT_SECRET`, `INTERNAL_SECRET`
- `FRONTEND_ORIGIN` (used for gateway CORS)

## Run Locally

1. Prepare env file
```bash
cp .env.example .env
```

2. Build
```bash
./gradlew build -x test
```

3. Start infra and services
```bash
docker compose up --build
```

## Useful Commands

```bash
# run all tests
./gradlew test

# run tests for one service
./gradlew :trading-service:test

# compile one module
./gradlew :api-gateway:compileJava
```

## Notes

- The gateway is the public API entry point.
- Configuration is served from `config-repo` through `config-service`.
- Service boot order: `discovery-service` -> `config-service` -> other services.
