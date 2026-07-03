# home-banking-bff

`home-banking-bff` is the browser-facing backend for the future home banking UI.

It stays behind `api-gateway` and owns browser session behavior.

## Responsibility

- Start OAuth2/OIDC login with Keycloak.
- Maintain a browser session through an HttpOnly session cookie.
- Resolve the authenticated Keycloak subject through `identity-service`.
- Compose customer-facing responses from internal services.
- Forward the user's access token to protected internal services.

It does not own customer data, accounts, balances, identity links, or authentication credentials.

## Architecture

The service uses a lightweight ports-and-adapters structure.

It intentionally does not copy the full business-service architecture because it does not own persistence or core banking rules.

Current structure:

```txt
application/port/in
application/port/out
application/usecase
domain/model
infrastructure/adapter/in/web
infrastructure/adapter/out/account
infrastructure/adapter/out/customer
infrastructure/adapter/out/identity
infrastructure/config
```

DTOs are kept close to their boundary:

```txt
infrastructure/adapter/in/web/dto
infrastructure/adapter/out/identity/dto
```

Main use case:

```txt
GetAuthenticatedHomeContextUseCase
```

The controller handles HTTP and session details. The use case owns the home banking composition flow. Output adapters call internal services through Eureka service names.

## Local Runtime

Default local port:

```txt
8086
```

Public local path through gateway:

```txt
http://localhost:8085/web/**
```

Direct local service path:

```txt
http://localhost:8086/web/**
```

## Endpoints

```txt
GET /web/session
GET /web/me
GET /web/logout
```

`GET /web/session` is public and returns whether the current browser has an authenticated BFF session.

`GET /web/me` requires an authenticated browser session.

If no session exists, Spring Security redirects to Keycloak login.

`GET /web/logout` clears the local BFF session and starts OIDC logout at Keycloak. After Keycloak logout, the browser is redirected back to `/web/session`.

## Flow

```txt
Browser -> api-gateway -> home-banking-bff -> Keycloak login
Browser -> api-gateway -> home-banking-bff with HttpOnly cookie
home-banking-bff -> identity-service/customer-service/account-service with Bearer token
```

The browser does not send a `customerId` to decide which banking data to read.

The BFF resolves the customer from the authenticated identity:

```txt
Keycloak subject -> identity-service -> customerId
```

Use `home-banking-user` for local browser testing. `identity-admin` is an operational user for identity link administration, not the typical customer login.

## Logout Flow

```txt
Browser -> api-gateway -> home-banking-bff /web/logout
home-banking-bff -> Keycloak end-session endpoint
Keycloak -> api-gateway -> home-banking-bff /web/session
```

This matters because deleting only the BFF cookie does not fully log the user out. If the Keycloak session is still alive, the next login attempt can silently authenticate the same user again.

The local Keycloak client must allow these post logout redirects:

```txt
http://localhost:8085/web/session
http://localhost:8086/web/session
```

## Tests

```powershell
.\mvnw.cmd test
```

Current verified result:

```txt
5 tests passing
```
