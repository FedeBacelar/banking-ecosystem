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
POST /internal/onboarding/applications/{applicationId}/review/retry
POST /internal/onboarding/applications/{applicationId}/provisioning/retry
```

Security:

```txt
ONBOARDING_WRITE -> start applications and consume magic links
ONBOARDING_READ  -> read application metadata and validate continuations
ONBOARDING_READ  -> resolve completion status from a BFF-derived Keycloak subject
ONBOARDING_OPERATE -> retry failed review or provisioning work
```

The BFF service account has `ONBOARDING_READ` and `ONBOARDING_WRITE`, but not `ONBOARDING_OPERATE`. Operational retries are intentionally unavailable from the browser journey.

Outbound customer, account, and identity creation uses the dedicated `onboarding-orchestrator` service account with `*_PROVISION` roles. Those calls do not reuse the BFF or browser token.

## Tests

```powershell
.\mvnw.cmd test
```
