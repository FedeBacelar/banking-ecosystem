# api-gateway

API Gateway for the banking ecosystem.

This is a platform service. It does not own banking data and does not implement customer or account business rules.

## Responsibility

`api-gateway` is the HTTP entry point for external clients.

Current responsibilities:

- Route external HTTP requests to internal services.
- Keep public API paths centralized.
- Resolve service destinations through Eureka using logical service names.
- Validate JWT access tokens issued by Keycloak.
- Enforce route-level authorization rules for customer and account APIs.
- Forward browser session routes to `home-banking-bff`.

## Local Runtime

Default port:

```txt
8085
```

Base URL:

```txt
http://localhost:8085
```

## Current Routes

```txt
/api/customers/** -> lb://customer-service
/api/accounts/**  -> lb://account-service
/web/**           -> lb://home-banking-bff
```

The `lb://` prefix means the gateway resolves the target service through Eureka instead of using a fixed host and port.

The `/api` prefix is stripped before forwarding requests to business services.

## Security

`api-gateway` is configured as an OAuth2 Resource Server.

Token issuer:

```txt
http://localhost:8090/realms/banking-ecosystem
```

Current authorization rules:

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

The gateway reads Keycloak realm roles from the JWT `realm_access.roles` claim.

`/web/**` is passed through without a Bearer token because `home-banking-bff` owns browser login and session cookies.

## Configuration

The service reads operational configuration from Config Server.

Config source:

```txt
../config-repository/api-gateway.yaml
```

Local bootstrap config remains in:

```txt
src/main/resources/application.yaml
```

## Run

Recommended local startup order:

```txt
1. MySQL containers
2. keycloak
3. config-server
4. eureka-server
5. customer-service
6. account-service
7. identity-service
8. home-banking-bff
9. api-gateway
```

From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

## Test

```powershell
.\mvnw.cmd test
```
