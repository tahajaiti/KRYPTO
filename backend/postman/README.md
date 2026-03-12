# Postman Artifacts

Import these two files into Postman:

1. `KRYPTO.postman_collection.json`
2. `KRYPTO.postman_environment.json`

## Recommended Import Order

1. Import environment file
2. Import collection file
3. Select environment: **KRYPTO Local**

## Notes

- Base URL defaults to `http://localhost:8080` (api-gateway).
- Auth is cookie-based (`/api/auth/login` and `/api/auth/register` set HttpOnly cookies).
- Keep requests on the same host (`{{baseUrl}}`) so Postman cookie jar can send auth cookies.
- Internal routes use header `X-Internal-Secret` with `{{internalSecret}}`.

## Tip

If you prefer Bearer auth, set `jwtToken` in environment and enable the disabled `Authorization` headers where needed.
