# document-service

`document-service` stores document metadata and uploads file bytes to MinIO through the AWS S3 SDK.

## Current Status

Implemented.

Current capabilities:

- Uploads documents through an internal multipart API.
- Validates content type and size.
- Stores file bytes in MinIO.
- Stores metadata in `document_db`.
- Retrieves metadata by document id.
- Registers with Eureka.
- Reads configuration from Config Server.
- Validates Keycloak JWT access tokens as an OAuth2 Resource Server.

## Responsibility

The service answers:

```txt
Where is this document stored and what metadata was accepted?
```

It does not:

```txt
approve onboarding
review documents
own customer data
own account data
authenticate users
serve public document downloads
```

## API

```txt
POST /internal/documents
GET /internal/documents/{documentId}
```

## Security

```txt
DOCUMENT_WRITE -> upload documents
DOCUMENT_READ -> read document metadata
```

Swagger UI can authenticate against Keycloak using the `banking-swagger` client with Authorization Code and PKCE.

## Database

```txt
document_db.document
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
