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

`api-gateway`, `customer-service`, and `account-service` validate JWT access tokens issued by this realm.

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

## Realm Model

Current realm:

```txt
banking-ecosystem
```

The `master` realm is reserved for Keycloak administration. Application security belongs in the banking realm.

Current client:

```txt
banking-api
```

This public client is used to request local access tokens for API testing.

Current realm roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
```

These roles represent API capabilities. They are intentionally more specific than generic roles such as `USER` or `ADMIN`.

## Local Test Users

```txt
api-tester
customer-reader
customer-writer
account-reader
```

Local test users exist only for local token testing.

```txt
api-tester      -> all current API roles
customer-reader -> CUSTOMER_READ
customer-writer -> CUSTOMER_READ, CUSTOMER_WRITE
account-reader  -> ACCOUNT_READ
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

`api-gateway`, `customer-service`, and `account-service` are configured as OAuth2 Resource Servers.

The gateway owns the external API access rules. Business services also validate tokens directly so direct service access is not trusted by default.
