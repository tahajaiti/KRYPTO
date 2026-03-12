# trading-service

Trading engine for KRYPTO coins with market and limit orders.

## Features

- Place buy/sell market and limit orders
- Match orders by price-time priority
- Support partial fills
- Record executed trades
- Update coin price after each trade
- Settle balances via wallet-service internal settlement endpoint
- Publish executed trade transactions to blockchain-service queue

## API

- `POST /api/trades/orders` place order
- `GET /api/trades/orders/{orderId}` get order by id (self/admin)
- `GET /api/trades/orders/me?page=0&size=20` current user orders
- `POST /api/trades/orders/{orderId}/cancel` cancel open order
- `GET /api/trades/me?page=0&size=20` current user trade history
- `GET /api/trades/coin/{coinId}?page=0&size=20` coin trade history (admin)

## Notes

- Trade fee in KRYP is configured by `trading.fee-rate`.
- Internal service calls are protected by `X-Internal-Secret`.
