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

It is not yet connected to `api-gateway`, `customer-service`, or `account-service`.

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

## Local Test User

```txt
api-tester
```

The local test user has all current API roles and exists only for local token testing.

## Future Integration

The planned integration path is:

```txt
client -> Keycloak -> access token
client -> api-gateway with Bearer token
api-gateway -> validates token
api-gateway -> routes to business services
```

In that model, `api-gateway` becomes an OAuth2 Resource Server and validates JWT access tokens issued by Keycloak.
