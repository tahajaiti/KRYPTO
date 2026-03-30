# KRYPTO

KRYPTO is a virtual cryptocurrency platform built with Spring Boot microservices.
Users receive KRYP (base currency), create custom coins, and trade them.

## Architecture

- Java 21, Spring Boot 4.0.3, Spring Cloud 2025.1.1
- Service discovery via Eureka
- Centralized configuration via Spring Cloud Config (`config-repo`)
- API routing via Spring Cloud Gateway
- Async messaging via RabbitMQ
- Caching via Redis
- PostgreSQL databases (one per business service + dedicated blockchain ledger)
- Docker Compose for local orchestration

## Databases

Each service manages its own PostgreSQL database for data isolation:
- `krypto_user` — user accounts and authentication
- `krypto_wallet` — wallet balances and transfer history
- `krypto_coin` — coin metadata, pricing, and snapshots
- `krypto_trading` — orders and trade execution records
- `krypto_blockchain` — blockchain ledger (blocks and transactions), persisted via JPA Hibernate

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

## Blockchain Service

The blockchain service implements a custom proof-of-work blockchain to audit and record all transactions in the system.

**Key Features:**
- Accepts transactions from API (`POST /api/blockchain/transactions`) and RabbitMQ events (`trading.exchange`, `market.exchange`)
- Mines pending transactions into blocks using configurable difficulty (default `3`)
- Persists entire block chain and transaction history in PostgreSQL (database: `krypto_blockchain`)
- Enforces idempotency via `sourceEventId` to prevent duplicate transaction processing
- Exposes chain verification endpoint (`GET /api/blockchain/verify`) to validate integrity at any time
- Auto-initializes genesis block on first startup

**Database Model:**
- `blocks` table: immutable block records with hash, nonce, previous hash, and timestamp
- `chain_transactions` table: transaction records with status (`PENDING`/`MINED`), source event ID for idempotency, and value fields
- Relationships: each block references its transactions via foreign key

**Configuration** (from `config-repo/blockchain-service.yml`):
- `blockchain.difficulty` — proof-of-work difficulty (default `3`)
- `blockchain.max-transactions-per-block` — transaction batch size (default `5`)
- `blockchain.idempotency-window-size` — (default `10000`)

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
