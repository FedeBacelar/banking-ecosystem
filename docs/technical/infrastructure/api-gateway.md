# API Gateway

`api-gateway` is the external HTTP entry point for the local banking ecosystem.

It is a platform component. It does not own business data and does not replace the business services.

## Purpose

The gateway gives clients one stable entry point instead of exposing every internal service directly.

Current local entry point:

```txt
http://localhost:8085
```

Current route model:

```txt
/api/customers/** -> customer-service
/api/accounts/**  -> account-service
/web/**           -> home-banking-bff
```

## Discovery Integration

The gateway routes to services through Eureka.

Routes use logical service names:

```txt
lb://customer-service
lb://account-service
lb://home-banking-bff
```

This keeps routing independent from concrete service ports such as `8080` or `8081`.

The BFF route preserves the original host header. This is required for browser OAuth2 redirects because the user must stay on the public gateway URL instead of being redirected to the internal BFF address.

## External Surface

Only paths configured in the gateway are part of the public API surface.

Direct local access to internal services can still exist during development, but clients should use the gateway as the ecosystem entry point.

When a browser-facing BFF is added, the BFF should stay behind the gateway:

```txt
Browser / frontend
  -> api-gateway
    -> home-banking-bff
      -> internal services
```

The BFF does not replace the gateway as the public edge.

`identity-service` is treated as an internal service. It is consumed by backend components such as `home-banking-bff`, not exposed directly as a public gateway route.

## Security

The gateway is configured as an OAuth2 Resource Server.

It validates JWT access tokens issued by Keycloak:

```txt
http://localhost:8090/realms/banking-ecosystem
```

Current route-level authorization:

```txt
GET   /api/customers/** -> CUSTOMER_READ
POST  /api/customers/** -> CUSTOMER_WRITE
PATCH /api/customers/** -> CUSTOMER_WRITE

GET   /api/accounts/**  -> ACCOUNT_READ
POST  /api/accounts/**  -> ACCOUNT_WRITE
PATCH /api/accounts/**  -> ACCOUNT_WRITE

/web/** -> allowed through to home-banking-bff
```

Other HTTP methods for `/api/customers/**` and `/api/accounts/**` are denied by default.

The gateway allows `/web/**` without a Bearer token because the browser authentication flow is session-based and owned by `home-banking-bff`.

Keycloak realm roles are read from:

```txt
realm_access.roles
```

Spring Security authorities are created with the `ROLE_` prefix so route rules can use `hasRole(...)`.

Business services also validate JWT tokens directly. The gateway is the public entry point, but it is not the only security layer.

## Future Concerns

The gateway is also the natural place for additional cross-cutting HTTP concerns:

- Rate limiting.
- Request correlation and tracing.

Business rules must remain inside the owning business service.

See also:

```txt
docs/technical/architecture/web-access-model.md
```
