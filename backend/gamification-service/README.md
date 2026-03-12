# Gamification Service

Manages challenges, badges, and leaderboards for the KRYPTO platform. Users earn badges through achievements and progress through challenges for rewards and recognition.

## Features

### Challenges
- **Daily/Weekly/Seasonal challenges** with configurable metrics
- **Progress tracking** per user with auto-completion when target is reached
- **Reward-based system** with points for completion
- **Challenge types**:
  - TOTAL_TRADES: Track number of trades
  - TOTAL_VOLUME: Track total trading volume
  - TOTAL_NOTIONAL_VALUE: Track total value traded in KRYP
  - COINS_CREATED: Track number of coins created
  - WALLET_VALUE: Track wallet net worth

### Badges
- **5 tier system**: BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
- **Automatic award logic** when challenges are completed
- **User badge tracking** with awarded timestamp
- **Per-user badge queries** for leaderboards and profiles

### Leaderboard
- **Redis-cached** for performance (60s TTL)
- **Ranked by points** (badges + challenge completions)
- **Top-N pagination** support (limit 1-100)
- **Rank display** with user stats

### RabbitMQ Integration
- **Consumes** `trading.exchange/trade.executed` events from trading-service
- **Consumes** `market.exchange/market.simulated` events from coin-service
- **Updates challenge progress** based on trade metrics
- **Triggers badge awards** on challenge completion

## API Endpoints

### Challenges
- `GET /api/challenges` - Get all active challenges
- `GET /api/challenges/type/{type}` - Get challenges by type (DAILY, WEEKLY, SEASONAL)
- `GET /api/challenges/{challengeId}` - Get specific challenge details
- `GET /api/challenges/user/my-challenges` - Get current user's challenges
- `GET /api/challenges/{challengeId}/user/my-progress` - Get current user's progress on specific challenge

### Badges
- `GET /api/badges` - Get all active badges
- `GET /api/badges/{badgeId}` - Get specific badge details
- `GET /api/badges/user/my-badges` - Get current user's earned badges
- `GET /api/badges/user/{userId}` - Get specific user's earned badges

### Leaderboard
- `GET /api/leaderboard?limit=10` - Get gamification leaderboard (cached, 60s TTL)

## Configuration

### Cache Configuration
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 60s
```

### Database
- Service: `krypto_gamification` on PostgreSQL
- Schemas: challenges, badges, user_challenges, user_badges
- Entities extend BaseEntity with UUID primary keys

### RabbitMQ Topology
- **Queues**: `gamification.events.queue` (durable)
- **Bindings**:
  - `trading.exchange` / `trade.executed` → `gamification.events.queue`
  - `market.exchange` / `market.simulated` → `gamification.events.queue`

## Service Integrations

### User Service
- Validates user existence via JWT tokens
- Uses shared JwtTokenProvider from common module

### Trading Service
- Receives trade event notifications
- Extracts notional value and user ID for challenge updates

### Coin Service
- Receives market simulation events
- Could be extended to track market-related challenges

## Database Schema

### challenges
- id (UUID, PK)
- name (VARCHAR)
- description (TEXT, nullable)
- type (VARCHAR: DAILY, WEEKLY, SEASONAL)
- targetValue (BIGINT)
- rewardPoints (INT)
- metric (VARCHAR: TOTAL_TRADES, TOTAL_VOLUME, etc.)
- active (BOOLEAN, default true)
- createdAt, updatedAt (TIMESTAMP)

### badges
- id (UUID, PK)
- name (VARCHAR, unique)
- description (TEXT, nullable)
- icon (VARCHAR)
- points (INT)
- tier (VARCHAR: BRONZE, SILVER, GOLD, PLATINUM, DIAMOND)
- active (BOOLEAN, default true)
- createdAt, updatedAt (TIMESTAMP)

### user_challenges
- id (UUID, PK)
- userId (VARCHAR)
- challengeId (UUID, FK)
- progress (BIGINT)
- completed (BOOLEAN)
- completedAt (TIMESTAMP, nullable)
- startedAt (BIGINT, unix timestamp)
- unique(userId, challengeId)
- createdAt, updatedAt (TIMESTAMP)

### user_badges
- id (UUID, PK)
- userId (VARCHAR)
- badgeId (UUID, FK)
- awardedAt (BIGINT, unix timestamp)
- unique(userId, badgeId)
- createdAt, updatedAt (TIMESTAMP)

## Service Pattern

- **Interface + Implementation pattern**: ChallengeService (interface) / ChallengeServiceImpl (impl)
- **Repository pattern**: JpaRepository for data access
- **DTO pattern**: Response DTOs for API contracts
- **Mapper pattern**: MapStruct for entity-to-DTO mapping
- **Exception handling**: Global exception handler with standard error responses
- **Caching**: Redis for leaderboard queries
- **Messaging**: RabbitMQ for async trade event processing

## Testing

- Integration tests with H2 in-memory database
- Simple cache type in test configuration (no Redis dependency)
- RabbitMQ listener tested via event injection
- Service layer tests for challenge/badge logic

## Development

### Build
```bash
./gradlew :gamification-service:build
```

### Test
```bash
./gradlew :gamification-service:test
```

### Run
```bash
./gradlew :gamification-service:bootRun
```

## Future Enhancements

- Challenge reset scheduling (daily/weekly)
- Badge tier progression tracking
- User achievement statistics
- Challenge difficulty levels
- Seasonal challenge rotations
- Leaderboard filtering (time period, metric)
- Challenge notifications
- Achievement milestone badges
