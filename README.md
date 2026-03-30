# KRYPTO

KRYPTO is a virtual cryptocurrency platform built with Spring Boot microservices.
Users receive KRYP (base currency), create custom coins, and trade them with live market matching and a blockchain ledger for audit transparency.

## Technology Stack

- **Java 21** + **Spring Boot 4.0.3** microservices
- **Spring Cloud** (Gateway, Config, Eureka)
- **PostgreSQL** for persistent data stores (separate DB per service + blockchain ledger)
- **RabbitMQ** for async event messaging
- **Redis** for caching
- **Angular 19** frontend with Tailwind CSS
- **Docker Compose** for local orchestration

## Architecture

The platform consists of eight services:

| Service | Port | Responsibility |
|---|---:|---|
| **api-gateway** | 8080 | Public HTTP entry point, request routing, CORS |
| **user-service** | 8081 | User auth, profiles, JWT token management |
| **wallet-service** | 8082 | KRYP transport, balance tracking, net worth |
| **coin-service** | 8083 | Token creation, market listing, price history |
| **trading-service** | 8084 | Order book, matching engine, trade execution |
| **blockchain-service** | 8085 | Transaction ledger, proof-of-work mining, audit trail |
| **discovery-service** | 8761 | Eureka service registry |
| **config-service** | 8888 | Spring Cloud Config server (reads `config-repo`) |

### Blockchain Service (8085)

A custom proof-of-work blockchain records all system transactions for auditability and transparency:

- **Persistent Storage**: blocks and transactions stored in PostgreSQL database (`krypto_blockchain`)
- **Transaction Pipeline**: accepts API requests and RabbitMQ events from trading/market services
- **Mining**: packs pending transactions into blocks with configurable difficulty (default `3`)
- **Idempotency**: enforces unique `sourceEventId` to prevent duplicate processing
- **Verification**: exposes admin endpoint to validate full chain integrity (hash links, PoW, transaction inclusion)
- **Genesis Initialization**: automatically creates genesis block on first startup

**Key Endpoints:**
- `POST /api/blockchain/transactions` — add pending transaction
- `GET /api/blockchain/latest` — fetch latest block
- `GET /api/blockchain?page=0&size=10` — paginated block explorer
- `GET /api/blockchain/verify` — validate chain integrity (admin)

**Databases:**
- `krypto_blockchain` — blocks and chain_transactions tables

## Project Layout

```
KRYPTO/
├── backend/                  # Spring Boot services
│   ├── blockchain-service/
│   ├── coin-service/
│   ├── trading-service/
│   ├── user-service/
│   ├── wallet-service/
│   ├── api-gateway/
│   ├── discovery-service/
│   ├── config-service/
│   ├── common/              # Shared utilities, security, models
│   ├── config-repo/         # Centralized config files (YAML)
│   └── infrastructure/      # Database init scripts
│
├── frontend/                # Angular 19 SPA
│   └── src/
│       ├── app/
│       │   ├── features/    # Feature modules (auth, coin, trading, etc)
│       │   │   └── trading/
│       │   │       ├── pages/
│       │   │       │   ├── trading-page/    # Trading desk
│       │   │       │   └── leaderboard-page/ # Standalone leaderboard
│       │   │       └── components/          # Reusable trading components
│       │   ├── core/        # Services, guards, HTTP interceptors
│       │   └── layout/      # Shell layout and header
│       └── main.ts
│
└── docker-compose.yml       # Local dev environment orchestration
```

## Key Features

- **Permissionless Coin Creation** — any user can create a custom token with initial supply
- **Live Trading** — market orders (instant execution) and limit orders (order book matching)
- **Transparent Ledger** — all transactions recorded in a blockchain with proofs
- **Wallet Management** — send KRYP to other users, track portfolio net worth
- **Leaderboard** — dedicated page ranking traders by total volume and trade count
- **Real-time Price Updates** — coin price history stored and exposed via API
- **Markets Explorer** — searchable coin listing with sort controls and coin creation modal
- **Public Profiles** — view any user's trading stats, created coins, and watchlist

## Getting Started

### Prerequisites

- Docker Desktop
- Java 21
- Node.js 20+

### Run Locally

1. **Prepare environment**
   ```bash
   cp .env.example .env
   ```

2. **Start backend services + infrastructure**
   ```bash
   cd backend
   ./gradlew build -x test
   cd ..
   docker-compose up --build
   ```

3. **Start frontend** (in a new terminal)
   ```bash
   cd frontend
   npm install
   npm start
   ```

Frontend will be available at `http://localhost:4200`
Backend gateway at `http://localhost:8080`

### Environment Variables

Key configuration (see `.env.example`):
- `POSTGRES_USER`, `POSTGRES_PASSWORD` — database credentials
- `RABBITMQ_USER`, `RABBITMQ_PASSWORD` — message broker
- `JWT_SECRET`, `INTERNAL_SECRET` — signing keys
- `FRONTEND_ORIGIN` — CORS origin for gateway

## Development

### Build All Modules

```bash
cd backend && ./gradlew build -x test
```

### Run Tests

```bash
./gradlew test                              # all services
./gradlew :trading-service:test             # single service
```

### Useful Commands

```bash
# Compile one service
./gradlew :blockchain-service:compileJava

# Check for dependency updates
./gradlew dependencyUpdates

# Clean build artifacts
./gradlew clean
```

## Documentation

- [Backend README](./backend/README.md) — microservice architecture and configuration
- [Blockchain Service README](./backend/blockchain-service/README.md) — ledger design and API
- [Frontend README](./frontend/README.md) — Angular app structure and build instructions
- `backend/agent/` — development notes and project state
- `frontend/agent/` — frontend integration and manual guides

## Architecture Notes

- **Service Discovery**: all services register with Eureka at startup and discover each other
- **Configuration Server**: all services pull configuration from Spring Cloud Config (`config-repo`)
- **Database Per Service**: data isolation and schema ownership per domain
- **Async Events**: RabbitMQ pub/sub for trading events → blockchain audit trail
- **Gateway Auth**: JWT tokens validated at API Gateway; internal services use a shared secret
- **Blockchain Persistence**: full history persisted in PostgreSQL with JPA, genesis block auto-initialized