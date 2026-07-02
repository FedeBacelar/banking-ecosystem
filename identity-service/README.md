# identity-service

`identity-service` links authenticated external identities to internal banking customers.

It does not authenticate users and does not store passwords. Authentication is handled by Keycloak. Customer data is owned by `customer-service`.

## Responsibility

Resolve:

```txt
provider + providerSubject -> customerId
```

Current provider model:

```txt
KEYCLOAK
GOOGLE
```

The service currently supports the model for multiple providers, but local runtime authentication uses Keycloak.

## Main Entity

```txt
IdentityLink
- id
- customerId
- provider
- providerSubject
- status
- createdAt
- updatedAt
- version
```

Statuses:

```txt
PENDING_VERIFICATION
ACTIVE
DISABLED
```

## API

```txt
POST  /identity-links
GET   /identity-links/providers/{provider}/subjects/{providerSubject}
GET   /identity-links/customers/{customerId}
PATCH /identity-links/{identityLinkId}/activate
PATCH /identity-links/{identityLinkId}/disable
```

## Security

The service is an OAuth2 Resource Server.

Current roles:

```txt
IDENTITY_READ
IDENTITY_WRITE
```

Access rules:

```txt
GET   /identity-links/** -> IDENTITY_READ
POST  /identity-links/** -> IDENTITY_WRITE
PATCH /identity-links/** -> IDENTITY_WRITE
```

Health/info endpoints are public.

Swagger/OpenAPI can be exposed locally through centralized configuration:

```txt
banking.security.public-docs-enabled=true
```

Swagger UI supports OAuth2 login with Keycloak through the `banking-swagger` client and PKCE. Use `Authorize` in Swagger and log in with a user that has the required identity roles.

## Database

The service owns its own database:

```txt
identity_db
```

Local default port:

```txt
3309
```

Migrations:

```txt
src/main/resources/db/migration
```

## Local Run

Start infrastructure from the repository root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d
docker compose -f infra/keycloak/docker-compose.yml up -d
```

Start platform services:

```powershell
cd config-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd ..\eureka-server
.\mvnw.cmd spring-boot:run
```

Start `identity-service`:

```powershell
cd ..\identity-service
.\mvnw.cmd spring-boot:run
```

Swagger:

```txt
http://localhost:8082/swagger-ui.html
```

## Tests

```powershell
.\mvnw.cmd test
```
