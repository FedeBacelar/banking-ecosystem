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

`api-gateway`, `customer-service`, `account-service`, `identity-service`, `notification-service`, `document-service`, and `home-banking-bff` integrate with this realm.

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

`api-gateway` owns banking API routes such as customer, account, and BFF routes.

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
home-banking-bff
```

`banking-api` is used to request local access tokens for API testing.

`banking-swagger` is used by service Swagger UIs with Authorization Code and PKCE.

`home-banking-bff` is a confidential client used by the browser-facing backend with Authorization Code Flow.

Current realm roles:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
IDENTITY_READ
IDENTITY_WRITE
NOTIFICATION_WRITE
DOCUMENT_READ
DOCUMENT_WRITE
```

These roles represent API capabilities. They are intentionally more specific than generic roles such as `USER` or `ADMIN`.

## Local Test Users

```txt
banking-admin
home-banking-user
```

Local users have separate responsibilities. The realm intentionally avoids one operational user per service.

```txt
banking-admin      -> local operational/API testing across current services
home-banking-user  -> browser login through home-banking-bff
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

`api-gateway`, `customer-service`, `account-service`, `identity-service`, `notification-service`, and `document-service` are configured as OAuth2 Resource Servers.

The gateway owns the external API access rules. Business services also validate tokens directly so direct service access is not trusted by default.

`home-banking-bff` is configured as an OAuth2 Client. It creates a browser session and uses the user's access token when calling protected internal services.

## Swagger Client

`banking-swagger` is a public local client for Swagger UI.

It is configured for Authorization Code with PKCE and local Swagger redirect URLs:

```txt
http://localhost:8080/swagger-ui/oauth2-redirect.html
http://localhost:8081/swagger-ui/oauth2-redirect.html
http://localhost:8082/swagger-ui/oauth2-redirect.html
http://localhost:8083/swagger-ui/oauth2-redirect.html
http://localhost:8084/swagger-ui/oauth2-redirect.html
```

If Keycloak already has an existing Docker volume, the realm import file may not create this client automatically. Create it manually or recreate the local Keycloak volume.

## BFF Client

`home-banking-bff` is a confidential local client.

Local redirect URLs:

```txt
http://localhost:8085/web/login/oauth2/code/keycloak
http://localhost:8086/web/login/oauth2/code/keycloak
```

The local client secret is a development default only. Real secrets must come from outside the repository.

## Login Theme

The local realm uses a custom login theme:

```txt
banking
```

Theme files live in:

```txt
infra/keycloak/themes/banking/login
```

The theme customizes the Keycloak authentication UI. It does not replace Keycloak authentication behavior and does not implement banking business screens.

The Docker Compose file mounts the theme folder into Keycloak:

```txt
./themes:/opt/keycloak/themes:ro
```

Theme, template, and static caches are disabled in local Docker Compose to make theme iteration predictable.

Current theme coverage:

```txt
login
error
info
login-page-expired
logout-confirm
```

The imported local realm disables public self-registration, public password recovery, and Keycloak remember-me. Those capabilities are intentionally not exposed as generic login-screen actions because banking recovery and onboarding flows need stronger business controls.

If the realm already exists in the local Docker volume, set the login theme manually in the admin console or recreate the volume.
