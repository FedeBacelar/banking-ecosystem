# home-banking-bff Implementation

`home-banking-bff` is the browser-facing backend for the future home banking UI.

It does not own banking business data. It owns browser session behavior and composes data from internal services.

## Current Status

Implemented as a first functional BFF slice.

Current capabilities:

- Starts OAuth2/OIDC login with Keycloak.
- Maintains the browser session using an HttpOnly session cookie.
- Performs OIDC logout so the BFF session and Keycloak session are closed together.
- Resolves the authenticated Keycloak subject through `identity-service`.
- Reads the linked customer from `customer-service`.
- Reads customer accounts from `account-service`.
- Forwards the user's access token when calling protected internal services.
- Starts onboarding applications through `onboarding-service`.
- Consumes onboarding magic links and stores continuation tokens in an HttpOnly cookie.
- Validates the onboarding continuation cookie.

## Architecture

The BFF uses a lightweight ports-and-adapters structure:

```txt
application/port/in
application/port/out
application/usecase
domain/model
infrastructure/adapter/in/web
infrastructure/adapter/out/account
infrastructure/adapter/out/customer
infrastructure/adapter/out/identity
infrastructure/adapter/out/onboarding
infrastructure/adapter/out/security
infrastructure/config
```

DTOs are kept close to their boundary:

```txt
infrastructure/adapter/in/web/dto
infrastructure/adapter/out/identity/dto
```

This is intentionally lighter than the business services.

`home-banking-bff` does not own entities, repositories, migrations, or banking lifecycle rules. It owns browser session behavior and composition for the home banking experience.

Main implemented use case:

```txt
GetAuthenticatedHomeContextUseCase
```

## Local Runtime

Default local port:

```txt
8086
```

Gateway path:

```txt
http://localhost:8085/web/**
```

Direct local path:

```txt
http://localhost:8086/web/**
```

## Endpoints

```txt
GET /web/session
GET /web/me
GET /web/logout
POST /web/onboarding/applications
POST /web/onboarding/magic-links/consume
GET /web/onboarding/session
DELETE /web/onboarding/session
```

`GET /web/session` returns the current browser session state. It is public and can be used by a future frontend to know if the browser is already authenticated.

`GET /web/me` returns the authenticated home banking context: identity subject, linked customer, and current accounts.

`GET /web/logout` clears the local BFF session and logs out from Keycloak. After logout, the browser returns to `/web/session`.

`POST /web/onboarding/applications` starts onboarding for an unauthenticated applicant.

`POST /web/onboarding/magic-links/consume` consumes a magic link and writes the continuation token as an HttpOnly cookie.

`GET /web/onboarding/session` reads and validates the onboarding continuation cookie.

`DELETE /web/onboarding/session` clears the onboarding continuation cookie.

## Security

The browser authenticates with `home-banking-bff` through OAuth2/OIDC login.

After login, the browser keeps only the BFF session cookie. The BFF keeps access to the OAuth2 authorized client server-side and sends the access token to internal services as a Bearer token.

Logout must be federated through Keycloak. Clearing only the BFF cookie is not enough because Keycloak can still have an active identity provider session.

The onboarding endpoints are public because applicants do not have a banking user yet. The BFF still calls `onboarding-service` as a confidential OAuth2 client using client credentials. The browser never receives that internal access token.

## Tests

Current test command:

```powershell
cd home-banking-bff
.\mvnw.cmd test
```

Current verified result:

```txt
14 tests passing
```
