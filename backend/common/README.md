# common module

Shared library module used by all KRYPTO services.

## Purpose

Provides reusable cross-service building blocks:

- Base entities and DTO contracts
- Standard error model and exceptions
- Shared security utilities for JWT auth
- Shared event models for RabbitMQ communication

## Key Components

### Entities
- `BaseEntity`: UUID id, `createdAt`, `updatedAt`

### DTOs
- `ApiResponse<T>`
- `PageResponse<T>`

### Exceptions
- `BusinessException`
- `ErrorCode`
- `ResourceNotFoundException`

### Security
- `JwtTokenProvider`
- `JwtAuthenticationFilter`
- `JwtPrincipal`
- `AuthorizationUtils`

### Events
- `BaseEvent`
- `UserRegisteredEvent`

## Dependency Usage

Each service imports this module through Gradle:

```kotlin
implementation(project(":common"))
```

## Notes

- Keep this module framework-light and reusable.
- Business logic should stay inside individual services.
