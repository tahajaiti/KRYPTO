# blockchain-service

Handles KRYPTO blockchain simulation with transaction intake (sync + async), proof-of-work mining, and integrity verification.

## What it does

- accepts transactions from API and RabbitMQ events
- keeps pending transactions and mines blocks when threshold is reached
- exposes block explorer endpoints (latest + paginated list)
- verifies full chain integrity (links, hashes, transaction hashes, PoW)

## Endpoints

- `POST /api/blockchain/transactions` add a pending transaction
- `POST /api/blockchain/mine` mine pending transactions into a new block (admin)
- `GET /api/blockchain/latest` get latest block
- `GET /api/blockchain?page=0&size=10&sort=index,desc` paginated block explorer
- `GET /api/blockchain/verify` validate chain integrity (admin)

## Security

- Uses shared JWT auth from `common.security.JwtAuthenticationFilter`.
- All endpoints require authentication.
- `mine` and `verify` are admin-only.
- For `POST /transactions`, `fromUserId` is bound to authenticated user unless caller is ADMIN.

## Messaging

Consumes queue:
- `blockchain.transaction.queue`

Bindings:
- `trading.exchange` / `trade.executed`
- `market.exchange` / `market.simulated`

Listener ingestion behavior:
- accepts both typed DTO payload and map/json payload
- maps producer payloads to blockchain request model
- supports optional idempotency metadata:
	- `sourceEventId`
	- `eventTimestamp` (epoch millis)
- rethrows processing failures so Rabbit listener retry policy is applied

Retry and DLQ:
- queue: `blockchain.transaction.queue`
- retry: 3 attempts with backoff (configured via `spring.rabbitmq.listener.simple.retry.*`)
- exhausted messages are dead-lettered to:
	- exchange: `blockchain.dlx.exchange`
	- queue: `blockchain.transaction.dlq`
	- routing key: `blockchain.transaction.failed`

## Transaction payload contract

`AddTransactionRequest`:
- `type` (required)
- `amount` (required, positive)
- `fromUserId`
- `toUserId`
- `coinSymbol`
- `fee` (optional, non-negative)
- `sourceEventId` (optional, recommended for idempotency)
- `eventTimestamp` (optional epoch millis)

## Verification guarantees

`/verify` checks:
- previous-hash links between adjacent blocks
- block hash recomputation
- proof-of-work prefix for every non-genesis block
- transaction hash integrity inside every block
- genesis invariants (`index=0`, `previousHash=0`)

## Pagination and sorting

`GET /api/blockchain` supports sorting by:
- `index`
- `timestamp`

Default sort fallback is `index,desc`.

## Configuration

Main properties (from config-repo):
- `blockchain.difficulty` (default `3`)
- `blockchain.max-transactions-per-block` (default `5`)
- `blockchain.idempotency-window-size` (default `10000`)
- `spring.datasource.*` for PostgreSQL connectivity
- `spring.jpa.*` for schema management and ORM behavior

Persistence behavior:
- chain and transactions are persisted in PostgreSQL via JPA entities
- pending transactions are stored with status tracking (`PENDING` -> `MINED`)
- idempotency is enforced with `sourceEventId` uniqueness in database
- genesis block is initialized once at startup if storage is empty