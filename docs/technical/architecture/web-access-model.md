# Web Access Model

This document defines the accepted web access model for the banking ecosystem.

## Public Entry Points

The ecosystem can have more than one public entry point, but each one must have a clear responsibility.

Recommended production-style model:

```txt
frontend public endpoint -> home banking UI
api public endpoint      -> api-gateway
auth public endpoint     -> Keycloak
```

Example:

```txt
https://home.bank.example -> frontend
https://api.bank.example  -> api-gateway
https://auth.bank.example -> Keycloak
```

`api-gateway` is the public edge for banking APIs.

Keycloak is the public identity provider endpoint for OAuth2/OpenID Connect login, token, logout, and discovery flows.

Keycloak should not be routed as a business API through `api-gateway`.

## Principle

Browser-facing applications should not call internal business services directly.

When a Backend for Frontend is added, it should also stay behind the gateway:

```txt
Browser / frontend
  -> api-gateway
    -> home-banking-bff
      -> internal services
```

## Responsibility Split

`api-gateway` owns the public HTTP entry point.

It is responsible for routing external requests into the ecosystem and for cross-cutting edge concerns such as authentication checks, CORS, request headers, rate limiting, and observability.

`home-banking-bff` owns browser-specific backend behavior.

It should translate frontend needs into backend calls, manage the browser session strategy, and compose customer-facing responses.

Business services remain internal capability owners.

They own business rules, persistence, and service-level authorization.

## Target Flow

```txt
Browser
  -> api-gateway
    -> home-banking-bff
      -> onboarding-service
      -> identity-service
      -> customer-service
      -> account-service
```

`identity-service` is not exposed as a direct public gateway route in the current model.

It is consumed internally by `home-banking-bff` to resolve an authenticated identity to a banking customer.

`onboarding-service` is also consumed internally by `home-banking-bff` for applicant flows that start before authentication.

## Why The BFF Does Not Replace The Gateway

The BFF is not the public edge.

The BFF is a backend tailored to one frontend experience. The gateway remains the consistent entry point for public HTTP traffic.

This avoids exposing multiple public backends and keeps edge concerns centralized.

## Current State

The current implemented gateway routes are:

```txt
/api/customers/** -> customer-service
/api/accounts/**  -> account-service
/web/**           -> home-banking-bff
```

The BFF route is intentionally allowed through the gateway without a Bearer token:

```txt
Browser -> api-gateway -> home-banking-bff
```

The browser authenticates with the BFF session cookie. The BFF then forwards the user's access token to protected internal services.

For onboarding, the browser does not have a user session yet. The BFF exposes `/web/onboarding/**`, stores the onboarding continuation token in an HttpOnly cookie, and calls `onboarding-service` with its confidential client credentials.

Business services should continue validating tokens even when requests enter through the gateway.

Local development currently uses:

```txt
Frontend: http://localhost:4200
Gateway:  http://localhost:8085
BFF:      http://localhost:8085/web/**
Keycloak: http://localhost:8090
```

The Angular development server proxies `/web` so browser code keeps using the same BFF route shape that production will use.

In local development the proxy target is `home-banking-bff` directly to avoid Vite proxy parsing issues with gateway responses. The intended runtime entrypoint remains `api-gateway /web/**`.
