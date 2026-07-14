# onboarding-service

Digital onboarding workflow service for applicants who start without a banking user, customer, identity link, or account.

## Responsibilities

- Own onboarding applications.
- Generate and consume one-time email magic links.
- Issue hashed continuation tokens for the BFF onboarding cookie flow.
- Enforce the initial onboarding state machine.
- Persist application state in `onboarding_db`.
- Request email delivery through `notification-service`.

It does not own customer records, bank accounts, document file storage, identity links, or Keycloak authentication.

## Local Requirements

Start local dependencies from the repository root:

```powershell
docker compose -f infra/mysql/docker-compose.yml up -d onboarding-mysql notification-mysql
```

Run platform services and `notification-service` before testing email delivery.

Run the service:

```powershell
cd onboarding-service
.\mvnw.cmd spring-boot:run
```

Swagger:

```txt
http://localhost:8087/swagger-ui.html
```

## API

```txt
POST /internal/onboarding/applications
GET /internal/onboarding/applications/{applicationId}
POST /internal/onboarding/magic-links/consume
POST /internal/onboarding/continuations/validate
POST /internal/onboarding/completion-status
```

Security:

```txt
ONBOARDING_WRITE -> start applications and consume magic links
ONBOARDING_READ  -> read application metadata and validate continuations
ONBOARDING_READ  -> resolve completion status from a BFF-derived Keycloak subject
```

## Tests

```powershell
.\mvnw.cmd test
```
