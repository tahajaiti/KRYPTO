# wallet-service

manages user wallets, balances, and KRYP transfers.

## port

`8082` (direct) / `8080` via api-gateway at `/api/wallets/**`

## database

postgresql - `krypto_wallets`

## security

- jwt validated per request using shared `common.security.JwtAuthenticationFilter`
- current user and role checks use shared `common.security.AuthorizationUtils`
- all `/api/wallets/**` endpoints require authentication

## endpoints

| method | path | auth | description |
|--------|------|------|-------------|
| GET | `/api/wallets/me` | any user | get current user's wallet |
| GET | `/api/wallets/{userId}` | self or ADMIN | get wallet by user id |
| GET | `/api/wallets/{userId}/balances` | self or ADMIN | list balances |
| GET | `/api/wallets/me/net-worth` | any user | calculate current user's net worth in KRYP |
| GET | `/api/wallets/{userId}/net-worth` | self or ADMIN | calculate target user's net worth in KRYP |
| POST | `/api/wallets/transfer/kryp` | any user | transfer KRYP to another user |

## events consumed

| exchange | routing key | queue | event |
|----------|-------------|-------|-------|
| user.exchange | user.registered | wallet.user.registered.queue | UserRegisteredEvent |

on user registration, wallet-service auto-creates a wallet and seeds KRYP balance using `krypto.initial-balance`.

## notes

- `Wallet` and `WalletBalance` both extend `BaseEntity`
- `KRYP` is represented as `WalletBalance` with `coinId = null` and `symbol = "KRYP"`
- cross-service references are by id only
- net worth uses `coin-service` batch price endpoint (`POST /api/coins/prices/batch`) to fetch all coin prices in a single internal API request (N+1 optimization), falling back to `0` if unavailable
