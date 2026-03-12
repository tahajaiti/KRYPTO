# coin-service

manages coin creation, listing/search, and current price lookup.

## port

`8083` (direct) / `8080` via api-gateway at `/api/coins/**`

## database

postgresql - `krypto_coins`

## security

- jwt validated per request using shared `common.security.JwtAuthenticationFilter`
- all write and coin detail/list endpoints require authentication
- `GET /api/coins/{id}/price` is intentionally open for internal/public valuation lookups (wallet-service net worth)

## endpoints

| method | path | description |
|--------|------|-------------|
| POST | `/api/coins` | create a new coin (charges KRYP creation fee via wallet-service) |
| GET | `/api/coins` | list/search active coins (paginated, direct `PageResponse`) |
| GET | `/api/coins/{id}` | get coin details |
| GET | `/api/coins/{id}/price` | get current price (used by wallet-service net worth) |

## notes

- coin symbol is normalized to uppercase
- coin creation fee is configured via `coin.creation-fee`
- coin creation deducts KRYP fee by calling `wallet-service` (`POST /api/wallets/{userId}/debit/kryp`)
- initial price is derived from `creationFee / initialSupply` with minimum floor `0.0001`
- market events simulation runs on a configurable scheduler to mimic volatility, pumps, and crashes
- each simulation tick updates coin prices, writes `PriceHistory`, and emits blockchain transaction events
- simulation controls live under `market-simulation.*` in centralized config
