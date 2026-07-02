# Keycloak

Keycloak is the local Identity Provider for the banking ecosystem.

It is an infrastructure component, not a business service. It will be responsible for identity and access management when security is introduced.

## Purpose

Keycloak will provide:

- User authentication.
- OAuth2/OpenID Connect token issuance.
- Client registration.
- Role and permission management.
- Future identity federation, such as Google login.

## Current Status

Keycloak is available as local infrastructure with a development realm.

It is used by `api-gateway` as the token issuer. It is not directly integrated with `customer-service` or `account-service` yet.

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

## Gateway Integration

Current integration path:

```txt
client -> Keycloak -> access token
client -> api-gateway with Bearer token
api-gateway -> validates token
api-gateway -> routes to business services
```

In this model, `api-gateway` is an OAuth2 Resource Server and validates JWT access tokens issued by Keycloak.
