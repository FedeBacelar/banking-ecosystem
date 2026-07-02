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
/customers/** -> lb://customer-service
/accounts/**  -> lb://account-service
```

Current non-routes:

```txt
identity-service is not exposed directly through api-gateway.
```

The accepted browser-facing model for a future BFF is:

```txt
Browser / frontend -> api-gateway -> banking-bff -> internal services
```

The BFF should not replace the gateway as the public edge.

Security issuer:

```txt
KEYCLOAK_ISSUER_URI=http://localhost:8090/realms/banking-ecosystem
```

Current authorization rules:

```txt
GET   /customers/** -> CUSTOMER_READ
POST  /customers/** -> CUSTOMER_WRITE
PATCH /customers/** -> CUSTOMER_WRITE

GET   /accounts/**  -> ACCOUNT_READ
POST  /accounts/**  -> ACCOUNT_WRITE
PATCH /accounts/**  -> ACCOUNT_WRITE
```

Other HTTP methods for `/customers/**` and `/accounts/**` are denied by default.

## Tests

Current test command:

```powershell
cd api-gateway
.\mvnw.cmd test
```

Current verified result:

```txt
9 tests passing
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
8. api-gateway
```
