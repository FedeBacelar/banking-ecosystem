# Swagger OAuth2

Swagger UI is configured to authenticate against Keycloak using OAuth2 Authorization Code with PKCE.

This keeps API documentation usable while preserving protected endpoints.

## Scope

Swagger OAuth2 is configured for service-level documentation:

```txt
customer-service
account-service
identity-service
```

It is not configured for `api-gateway`.

The gateway documents and exposes the public edge separately. Service Swagger documents each service contract.

## Local Client

Keycloak contains a public client for Swagger:

```txt
banking-swagger
```

The client uses:

```txt
Authorization Code flow
PKCE S256
```

Local redirect URIs:

```txt
http://localhost:8080/swagger-ui/oauth2-redirect.html
http://localhost:8081/swagger-ui/oauth2-redirect.html
http://localhost:8082/swagger-ui/oauth2-redirect.html
```

## Usage

1. Start Keycloak.
2. Start Config Server.
3. Start the service you want to test.
4. Open the service Swagger UI.
5. Click `Authorize`.
6. Use the `banking-swagger` client.
7. Log in with a local Keycloak user that has the required realm roles.
8. Execute protected endpoints from Swagger.

Example service URLs:

```txt
http://localhost:8080/swagger-ui.html
http://localhost:8081/swagger-ui.html
http://localhost:8082/swagger-ui.html
```

## Required Roles

Swagger does not bypass authorization.

The logged-in Keycloak user still needs the API roles required by each endpoint:

```txt
CUSTOMER_READ
CUSTOMER_WRITE
ACCOUNT_READ
ACCOUNT_WRITE
IDENTITY_READ
IDENTITY_WRITE
```

## Existing Keycloak Volumes

The realm import file is applied when Keycloak initializes a new realm.

If Keycloak was already running with an existing Docker volume before `banking-swagger` was added, the client may not exist yet.

In that case, either create the `banking-swagger` client manually in Keycloak or recreate the local Keycloak volume.
