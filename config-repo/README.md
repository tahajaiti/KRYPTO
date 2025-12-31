# KRYPTO Config Repository

This folder contains centralized configuration for all KRYPTO microservices.

## Structure

| File               | Description                                                 |
| ------------------ | ----------------------------------------------------------- |
| `application.yml`  | Shared config for **all** services (Eureka, Redis, logging) |
| `gateway.yml`      | Gateway-specific config (routes, rate limiting, CORS)       |
| `auth-service.yml` | Auth service config (DB, security settings)                 |

## How It Works

1. **Config Server** mounts this folder and serves configs to services
2. **Vault** provides secrets (passwords, tokens) that get merged with these configs
3. Services request their config via: `http://config:8888/{service-name}/default`

## Adding a New Service

1. Create `{service-name}.yml` in this folder
2. Add service-specific settings
3. Secrets go in Vault under `secret/{service-name}`
