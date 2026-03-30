# KRYPTO Frontend

Angular 21 single-page application for the KRYPTO simulated crypto trading platform.

## Tech Stack

- **Angular 21.1.x** with standalone components and signals
- **TypeScript 5.9.x** with strict mode
- **TailwindCSS v4** with custom design tokens
- **ng-icons** for Heroicons integration
- **RxJS** with reactive patterns (signals, forkJoin, switchMap)

## Design System

Dark mode only with emerald/cyan neon accents:

- `krypto-card`, `krypto-card-header`, `krypto-stat-card` — card variants
- `krypto-btn-primary` (emerald gradient), `krypto-btn-secondary` (bordered)
- `krypto-label` — uppercase tracking labels
- `krypto-fade-in`, `krypto-slide-up` — entry animations
- **Font**: Space Grotesk for headings, system stack for body

## Architecture

```
src/app/
├── core/                    # guards, services, http utils
│   ├── guards/              # auth.guard, guest.guard, admin.guard
│   ├── http/                # API response models, error utilities
│   ├── services/            # SyncService (global refresh trigger)
│   └── tutorial/            # TutorialService (8-step onboarding)
├── features/
│   ├── auth/                # login, register, profile, public-profile
│   ├── admin/               # admin dashboard (admin-only)
│   ├── wallet/              # portfolio (balances, net worth, transfers)
│   ├── coin/                # markets listing, coin detail, coin creation modal
│   ├── trading/             # trading desk, leaderboard, order components
│   ├── blockchain/          # chain pulse API integration
│   └── home/                # aggregated dashboard with market overview
└── layout/
    ├── shell-layout/        # app shell (header + content + footer)
    └── components/
        ├── app-header/      # KRYP balance pill, mobile hamburger, nav links
        ├── app-footer/      # footer
        └── tutorial-overlay/ # step-by-step tutorial modal
```

## Pages

| Route | Page | Description |
|-------|------|-------------|
| `/` | Home | Market leaders, chain pulse, top traders, quick actions |
| `/login` | Login | JWT cookie authentication |
| `/register` | Register | Account creation |
| `/profile` | Profile | Settings, watchlist, community tabs, net worth |
| `/u/:username`, `/users/:id` | Public Profile | Trading stats, coins created, watchlist, net worth |
| `/markets` | Markets | Searchable coin listing, sidebar detail, coin creation modal |
| `/markets/:id` | Coin Detail | SVG price chart with hover tracking, creator card |
| `/trade` | Trading Desk | Order form, market snapshot, my orders, recent trades |
| `/leaderboard` | Leaderboard | Ranked trader table with volume, notional, trade count |
| `/portfolio` | Portfolio | Wallet balances, net worth, KRYP transfers |
| `/admin` | Admin | User and coin management (ADMIN only) |

## Development

```bash
# install dependencies
npm install

# start dev server (http://localhost:4200)
npm start

# production build
npx ng build

# run tests
npx ng test
```

## API Integration

All API calls go through the backend api-gateway at `http://localhost:8080`.
Auth uses http-only cookies (access_token + refresh_token) — no localStorage.

**API Services:**
- `AuthService` — login, register, logout, refresh, updateProfile
- `UserApiService` — getUserById, getUserByUsername, searchUsers
- `WalletApiService` — getCurrentWallet, getCurrentNetWorth, getUserNetWorth, transfer
- `CoinApiService` — listCoins, getCoinById, getCoinPrice, getCoinsByCreator, getWatchedCoins
- `TradingApiService` — placeOrder, getMyOrders, getMyTrades, getLeaderboard, cancelOrder
- `BlockchainApiService` — getLatestBlock

## Key Design Decisions

- **Signals over BehaviorSubjects** for component state management
- **forkJoin for parallel requests** to avoid N+1 waterfalls (profile pages)
- **Lazy-loaded routes** with auth/guest/admin guards
- **Feature-first folder structure** with separate api, models, pages, components per feature
- **External templates only** — no inline HTML or CSS
- **Modal pattern** for coin creation (keeps market page clean)
- **Searchable dropdown** for coin selector in trading order form
