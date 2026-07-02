# identity-service

`identity-service` links authenticated external identities to internal banking customers.

## Current Status

Implemented.

Current capabilities:

- Stores identity links in `identity_db`.
- Resolves an active identity link by provider and provider subject.
- Lists identity links for a customer.
- Creates identity links.
- Activates and disables identity links.
- Registers with Eureka.
- Reads configuration from Config Server.
- Validates Keycloak JWT access tokens as an OAuth2 Resource Server.

## Responsibility

The service answers:

```txt
Which banking customer belongs to this authenticated identity?
```

It does not:

```txt
authenticate users
store passwords
own customer profile data
create customers
perform KYC
register new digital banking users
```

## Core Mapping

```txt
provider + providerSubject -> customerId
```

Example:

```txt
KEYCLOAK + keycloak-sub-abc -> customerId
```

`customerId` is a logical reference to `customer-service`.

There is no physical database foreign key across service databases.

## API

```txt
POST  /identity-links
GET   /identity-links/providers/{provider}/subjects/{providerSubject}
GET   /identity-links/customers/{customerId}
PATCH /identity-links/{identityLinkId}/activate
PATCH /identity-links/{identityLinkId}/disable
```

## Security

```txt
IDENTITY_READ  -> read/resolve identity links
IDENTITY_WRITE -> create/update identity links
```

Swagger UI can authenticate against Keycloak using the `banking-swagger` client with Authorization Code and PKCE.

## Database

```txt
identity_db.identity_link
```

The table is documented in:

```txt
docs/database/schema.dbml
```

## Tests

Run from the service folder:

```powershell
.\mvnw.cmd test
```
