# Keycloak

Keycloak is the local Identity Provider for the banking ecosystem.

It is an infrastructure component, not a business service. It is responsible for local identity and access management.

## Purpose

Keycloak provides:

- User authentication.
- OAuth2/OpenID Connect token issuance.
- Client registration.
- Role and permission management.
- Future identity federation, such as Google login.

## Current Status

Keycloak is available as local infrastructure with a development realm.

`api-gateway`, `customer-service`, `account-service`, and `identity-service` validate JWT access tokens issued by this realm.

## Local Runtime

Infrastructure folder:

```txt
infra/keycloak/
```

Admin console:

```txt
http://localhost:8090
```

Default local admin credentials:

```txt
Username: admin
Password: admin
```

These defaults are for local development only.

## Public Access Model

Keycloak is exposed through its own auth endpoint.

It is not routed as a business API through `api-gateway`.

Recommended production-style endpoints:

```txt
https://api.bank.example  -> api-gateway
https://auth.bank.example -> Keycloak
```

This separation keeps banking API routing and identity-provider routing independent.

Keycloak owns OAuth2/OpenID Connect endpoints such as authorization, token exchange, logout, discovery metadata, and public keys.

`api-gateway` owns banking API routes such as customer, account, and future BFF routes.

## Realm Model

Current realm:

```txt
banking-ecosystem
```

The `master` realm is reserved for Keycloak administration. Application security belongs in the banking realm.

Current clients:

```txt
banking-api
banking-swagger
```

`banking-api` is used to request local access tokens for API testing.

`banking-swagger` is used by service Swagger UIs with Authorization Code and PKCE.

Current realm roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
IDENTITY_READ
IDENTITY_WRITE
```

These roles represent API capabilities. They are intentionally more specific than generic roles such as `USER` or `ADMIN`.

## Local Test Users

```txt
api-tester
customer-reader
customer-writer
account-reader
identity-admin
```

Local test users exist only for local token testing.

```txt
api-tester      -> all current API roles
customer-reader -> CUSTOMER_READ
customer-writer -> CUSTOMER_READ, CUSTOMER_WRITE
account-reader  -> ACCOUNT_READ
identity-admin  -> IDENTITY_READ, IDENTITY_WRITE
```

## Current Integration

Current request flow:

```txt
client -> Keycloak -> access token
client -> api-gateway with Bearer token
api-gateway -> validates token
api-gateway -> routes to business services
business service -> validates token again
```

`api-gateway`, `customer-service`, `account-service`, and `identity-service` are configured as OAuth2 Resource Servers.

The gateway owns the external API access rules. Business services also validate tokens directly so direct service access is not trusted by default.

## Swagger Client

`banking-swagger` is a public local client for Swagger UI.

It is configured for Authorization Code with PKCE and local Swagger redirect URLs:

```txt
http://localhost:8080/swagger-ui/oauth2-redirect.html
http://localhost:8081/swagger-ui/oauth2-redirect.html
http://localhost:8082/swagger-ui/oauth2-redirect.html
```

If Keycloak already has an existing Docker volume, the realm import file may not create this client automatically. Create it manually or recreate the local Keycloak volume.
