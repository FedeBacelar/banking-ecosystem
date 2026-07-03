# api-gateway Implementation

`api-gateway` is the current API Gateway component.

It is a platform service. It does not own banking business data.

## Current Status

Implemented.

Current capabilities:

- Runs a Spring Cloud Gateway WebFlux server.
- Reads route configuration from Config Server.
- Registers as a Eureka client.
- Routes customer requests to `customer-service`.
- Routes account requests to `account-service`.
- Routes browser-oriented BFF requests to `home-banking-bff`.
- Validates JWT access tokens issued by Keycloak.
- Enforces route-level authorization rules for customer and account APIs.

## Local Runtime

Default HTTP port:

```txt
8085
```

Base URL:

```txt
http://localhost:8085
```

## Configuration

Bootstrap file:

```txt
api-gateway/src/main/resources/application.yaml
```

Centralized configuration:

```txt
config-repository/api-gateway.yaml
```

Current routes:

```txt
/api/customers/** -> lb://customer-service
/api/accounts/**  -> lb://account-service
/web/**           -> lb://home-banking-bff
```

The `/api` prefix is part of the public gateway surface. It is stripped before forwarding to the business services, which still own their internal paths such as `/customers/**` and `/accounts/**`.

The `/web/**` route preserves the original host header so `home-banking-bff` can build OAuth2 redirects using the public gateway URL instead of its internal service address.

Current internal-only services:

```txt
identity-service is not exposed directly through api-gateway.
```

The accepted browser-facing model is:

```txt
Browser / frontend -> api-gateway -> home-banking-bff -> internal services
```

The BFF should not replace the gateway as the public edge.

Security issuer:

```txt
KEYCLOAK_ISSUER_URI=http://localhost:8090/realms/banking-ecosystem
```

Current authorization rules:

```txt
GET   /api/customers/** -> CUSTOMER_READ
POST  /api/customers/** -> CUSTOMER_WRITE
PATCH /api/customers/** -> CUSTOMER_WRITE

GET   /api/accounts/**  -> ACCOUNT_READ
POST  /api/accounts/**  -> ACCOUNT_WRITE
PATCH /api/accounts/**  -> ACCOUNT_WRITE

/web/** -> passed to home-banking-bff
```

Other HTTP methods for `/api/customers/**` and `/api/accounts/**` are denied by default.

The gateway does not require a Bearer token for `/web/**` because `home-banking-bff` owns the browser login and HttpOnly session cookie.

## Tests

Current test command:

```powershell
cd api-gateway
.\mvnw.cmd test
```

Current verified result:

```txt
10 tests passing
```

## Local Startup Order

```txt
1. infra/mysql
2. keycloak
3. config-server
4. eureka-server
5. customer-service
6. account-service
7. identity-service
8. home-banking-bff
9. api-gateway
```
