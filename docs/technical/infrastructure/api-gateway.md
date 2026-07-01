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

## Future Concerns

The gateway is the natural place for cross-cutting HTTP concerns:

- Authentication and authorization integration.
- Token validation.
- Route-level access rules.
- Rate limiting.
- Request correlation and tracing.

Business rules must remain inside the owning business service.
