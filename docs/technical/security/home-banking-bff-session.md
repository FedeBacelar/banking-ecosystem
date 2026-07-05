# BFF Session Security

This document describes the current browser session model for `home-banking-bff`.

## Model

The frontend should call banking APIs through the gateway:

```txt
Browser -> api-gateway -> home-banking-bff -> internal services
```

The browser does not store the access token directly.

Instead:

```txt
Browser -> home-banking-bff: HttpOnly session cookie
home-banking-bff -> internal services: Bearer access token
```

## Login Flow

1. The browser requests a BFF endpoint.
2. If there is no session, `home-banking-bff` redirects to Keycloak.
3. Keycloak authenticates the user.
4. Keycloak redirects back to the BFF callback.
5. The BFF creates a server-side session.
6. The browser receives an HttpOnly session cookie.
7. The BFF uses the authorized OAuth2 client to call internal services with the access token.

## Logout Flow

Logout is handled by the BFF through OIDC client-initiated logout:

```txt
Browser -> api-gateway -> home-banking-bff /web/logout
home-banking-bff -> Keycloak end-session endpoint
Keycloak -> api-gateway -> home-banking-bff /web/session
```

The BFF invalidates its local session and asks Keycloak to close the identity provider session.

This avoids a confusing local-development behavior: if only the BFF cookie is deleted, Keycloak can still silently authenticate the same user on the next request.

## Gateway Rule

`api-gateway` allows `/web/**` through without requiring a Bearer token.

That is intentional. Browser authentication is session-based at the BFF layer.

Business APIs such as `/api/customers/**` and `/api/accounts/**` still require Bearer tokens at the gateway.

Business services also validate Bearer tokens directly.

## Internal Resolution

`home-banking-bff` resolves the authenticated user with this chain:

```txt
Keycloak subject
  -> identity-service
    -> customerId
      -> customer-service
      -> account-service
```

`identity-service` remains internal and is not exposed as a direct public gateway route.

## Customer Data Boundary

The browser-facing customer flow must not accept a `customerId` from the browser.

For customer-facing endpoints, the BFF must derive the customer from the authenticated identity:

```txt
Keycloak subject -> identity link -> customerId
```

That is how the current `/web/me` endpoint returns "my" customer and "my" accounts.

`banking-admin` is only for local operational/API testing. It should not be used as the browser customer for the home banking composition.

Use `home-banking-user` for the browser customer flow.

The current business-service role checks are intentionally coarse-grained. A future hardening step should add ownership validation inside `customer-service` and `account-service` as defense in depth.

## Secrets

The BFF OAuth2 client is confidential and requires a client secret.

Local development may use a safe default, but real secrets must be injected from outside the repository.

## Local Verification

1. Open `http://localhost:8085/web/me`.
2. Login through Keycloak with `home-banking-user`.
3. If the identity is not linked, expect `409 IDENTITY_NOT_LINKED`.
4. Open `http://localhost:8085/web/session` and expect `authenticated: true`.
5. Open `http://localhost:8085/web/logout`.
6. After redirect, `http://localhost:8085/web/session` should return `authenticated: false`.
