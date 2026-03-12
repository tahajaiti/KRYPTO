# blockchain-service

Handles lightweight chain simulation for KRYPTO with transaction intake, block mining, and chain validation.

## Endpoints

- `POST /api/blockchain/transactions` add a pending transaction
- `POST /api/blockchain/mine` mine pending transactions into a new block (admin)
- `GET /api/blockchain/latest` get latest block
- `GET /api/blockchain?page=0&size=10` paginated block explorer
- `GET /api/blockchain/verify` validate chain integrity (admin)

## Security

- Uses shared JWT cookie validation from `common` via `JwtAuthenticationFilter`.
- All endpoints require authentication.
- Admin-only endpoints: mine and verify.

## Messaging

- Consumes `blockchain.transaction.queue` for async transaction intake.

## Notes

- Chain state is kept in memory for this phase.
- Proof-of-work is configured with `blockchain.difficulty`.
