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
/customers/** -> customer-service
/accounts/**  -> account-service
```

## Discovery Integration

The gateway routes to services through Eureka.

Routes use logical service names:

```txt
lb://customer-service
lb://account-service
```

This keeps routing independent from concrete service ports such as `8080` or `8081`.

## External Surface

Only paths configured in the gateway are part of the public API surface.

Direct local access to internal services can still exist during development, but clients should use the gateway as the ecosystem entry point.

When a browser-facing BFF is added, the BFF should stay behind the gateway:

```txt
Browser / frontend
  -> api-gateway
    -> banking-bff
      -> internal services
```

The BFF should not replace the gateway as the public edge.

`identity-service` is currently treated as an internal service. It is expected to be consumed by backend components such as a future `banking-bff`, not exposed directly as a public gateway route.

## Security

The gateway is configured as an OAuth2 Resource Server.

It validates JWT access tokens issued by Keycloak:

```txt
http://localhost:8090/realms/banking-ecosystem
```

Current route-level authorization:

```txt
GET   /customers/** -> CUSTOMER_READ
POST  /customers/** -> CUSTOMER_WRITE
PATCH /customers/** -> CUSTOMER_WRITE

GET   /accounts/**  -> ACCOUNT_READ
POST  /accounts/**  -> ACCOUNT_WRITE
PATCH /accounts/**  -> ACCOUNT_WRITE
```

Other HTTP methods for `/customers/**` and `/accounts/**` are denied by default.

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
