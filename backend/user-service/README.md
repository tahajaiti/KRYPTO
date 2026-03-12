# user-service

handles user registration, authentication (JWT via http-only cookies), profile management, and role-based access control.

## port

`8081` (direct) / `8080` via api-gateway at `/api/auth/**` and `/api/users/**`

## database

postgresql - `krypto_users`

## auth mechanism

tokens are stored in **http-only cookies**, never exposed in response bodies.

- `access_token` cookie: short-lived JWT (15 min), sent with every request (path: `/`)
- `refresh_token` cookie: long-lived opaque token (7 days), only sent to `/api/auth` endpoints
- inter-service calls can still use `Authorization: Bearer <token>` header as fallback

## endpoints

### auth (public - no authentication required)

| method | path | description |
|--------|------|-------------|
| POST | `/api/auth/register` | register a new user (sets auth cookies) |
| POST | `/api/auth/login` | login with username/email + password (sets auth cookies) |
| POST | `/api/auth/refresh` | refresh access token using refresh_token cookie |
| POST | `/api/auth/logout` | invalidate refresh token and clear cookies |

### users (authenticated)

| method | path | auth | description |
|--------|------|------|-------------|
| GET | `/api/users/me` | any user | get current user profile |
| PUT | `/api/users/me` | any user | update username or avatar |
| GET | `/api/users/{id}` | any user | get user by id (public profile) |
| GET | `/api/users` | ADMIN only | list all users (paginated) |
| PUT | `/api/users/{id}/role` | ADMIN only | change user role (PLAYER/ADMIN) |
| PUT | `/api/users/{id}/status` | ADMIN only | enable/disable user account |

### actuator

| method | path | description |
|--------|------|-------------|
| GET | `/actuator/health` | health check |
| GET | `/actuator/info` | service info |

## request/response examples

### register

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "satoshi",
  "email": "satoshi@krypto.com",
  "password": "nakamoto123"
}
```

response (201):
```json
{
  "success": true,
  "message": "registration successful",
  "data": {
    "user": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "username": "satoshi",
      "email": "satoshi@krypto.com",
      "avatar": null,
      "role": "PLAYER",
      "enabled": true,
      "createdAt": "2026-03-10T12:00:00Z"
    }
  },
  "timestamp": "2026-03-10T12:00:00Z"
}
```

cookies set:
```
Set-Cookie: access_token=eyJ...; Path=/; HttpOnly
Set-Cookie: refresh_token=550e8400...; Path=/api/auth; HttpOnly
```

### login

```
POST /api/auth/login
Content-Type: application/json

{
  "login": "satoshi",
  "password": "nakamoto123"
}
```

`login` field accepts either username or email.

### paginated user list (admin)

```
GET /api/users?page=0&size=20&sort=createdAt,desc
```

response:
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "numberOfElements": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "hasNext": true,
  "hasPrevious": false,
  "empty": false,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

### update profile

```
PUT /api/users/me
Content-Type: application/json

{
  "username": "satoshi_v2",
  "avatar": "https://example.com/avatar.png"
}
```

both fields are optional. only provided fields are updated.

## authentication flow

1. client sends `POST /api/auth/register` or `POST /api/auth/login`
2. server sets `access_token` (jwt, 15 min) and `refresh_token` (opaque uuid, 7 days) as http-only cookies
3. browser automatically sends cookies with every request
4. when access token expires, client sends `POST /api/auth/refresh` (refresh_token cookie is auto-included)
5. server rotates both tokens (old refresh token is invalidated, new cookies are set)
6. `POST /api/auth/logout` deletes refresh token from db and clears cookies

jwt claims: `sub` (username), `userId`, `role`

## roles

| role | permissions |
|------|-------------|
| PLAYER | register, login, view/edit own profile, view other profiles |
| ADMIN | everything PLAYER can do + list all users, change roles, enable/disable accounts |

## entities

### User (extends BaseEntity)
| field | type | notes |
|-------|------|-------|
| id | UUID | from BaseEntity |
| username | String | unique |
| email | String | unique |
| password | String | bcrypt hashed |
| avatar | String | nullable |
| role | PLAYER / ADMIN | default PLAYER |
| enabled | boolean | default true |
| createdAt | Instant | from BaseEntity |
| updatedAt | Instant | from BaseEntity |

### RefreshToken (extends BaseEntity)
| field | type | notes |
|-------|------|-------|
| id | UUID | from BaseEntity |
| token | String | opaque uuid string |
| user | User (FK) | many-to-one |
| expiresAt | Instant | ttl = 7 days |
| createdAt | Instant | from BaseEntity |
| updatedAt | Instant | from BaseEntity |

## events published (rabbitmq)

| exchange | routing key | event | trigger |
|----------|-------------|-------|---------|
| user.exchange | user.registered | UserRegisteredEvent | after successful registration |

event payload:
```json
{
  "eventId": "uuid",
  "eventType": "USER_REGISTERED",
  "occurredAt": "2026-03-10T12:00:00Z",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "satoshi",
  "email": "satoshi@krypto.com"
}
```

consumed by `wallet-service` to auto-create wallet with initial KRYP balance.

## config

configuration is pulled from `config-service` at startup. key values in `config-repo/user-service.yml`:

| property | default | description |
|----------|---------|-------------|
| jwt.secret | base64 encoded key | hmac signing key |
| jwt.access-token-expiration | 900000 (15 min) | access token ttl in ms |
| jwt.refresh-token-expiration | 604800000 (7 days) | refresh token ttl in ms |
| krypto.initial-balance | 10000 | starting KRYP for new users |

## package structure

```
com.krypto.user/
├── config/
│   ├── SecurityConfig          security filter chain, auth provider, password encoder
│   └── RabbitMQConfig          exchange declaration, json message converter
├── controller/
│   ├── AuthController          registration, login, refresh, logout (cookie-based)
│   └── UserController          profile crud, admin operations (paginated)
├── dto/
│   ├── request/
│   │   ├── RegisterRequest     username + email + password
│   │   ├── LoginRequest        login (username or email) + password
│   │   └── UpdateProfileRequest username + avatar (both optional)
│   └── response/
│       ├── AuthResponse        user info only (tokens go in cookies)
│       └── UserResponse        user profile without password
├── entity/
│   ├── User                    extends BaseEntity
│   ├── Role                    enum (PLAYER, ADMIN)
│   └── RefreshToken            extends BaseEntity
├── exception/
│   └── GlobalExceptionHandler  maps exceptions to api error responses
├── mapper/
│   └── UserMapper              mapstruct: User -> UserResponse
├── repository/
│   ├── UserRepository          find by username/email, exists checks
│   └── RefreshTokenRepository  find/delete by token, delete by user
├── security/
│   ├── JwtService              generate/validate tokens, extract claims
│   ├── JwtAuthenticationFilter reads jwt from cookie (fallback: auth header)
│   ├── CookieService           create/extract/clear http-only auth cookies
│   └── UserDetailsServiceImpl  loads user from db for spring security
└── service/
    ├── AuthService             register, login, refresh, logout + event publishing
    └── UserService             profile crud, role/status management (paginated)
```
