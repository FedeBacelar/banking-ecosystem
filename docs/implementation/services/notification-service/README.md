# notification-service

`notification-service` stores notification requests and sends templated email notifications.

## Current Status

Implemented.

Current capabilities:

- Stores email notification requests in `notification_db`.
- Renders known templates from `templateCode + variables`.
- Sends email through SMTP.
- Uses local Mailpit defaults (`localhost:1025`, without authentication or
  STARTTLS) for messages from both this service and Keycloak; deployed
  environments must inject their SMTP provider settings externally.
- Records delivery status, attempt count, sent timestamp, and last error.
- Derives variable schema, action-link validation, and audit redaction from the
  selected template instead of trusting the caller to classify content.
- Rejects missing and additional variables before rendering or persistence.
- Validates customer-name and expiration semantics per template.
- Binds idempotency keys to a one-way SHA-256 request fingerprint; conflicting
  reuse fails instead of returning another request's result.
- Registers with Eureka.
- Reads configuration from Config Server.
- Imports ignored local SMTP values from `notification-service/.env` when
  present; that file is for credential-free Mailpit overrides only. Process
  environment variables have higher precedence, and deployed SMTP secrets are
  injected outside the repository.
- Validates Keycloak JWT access tokens as an OAuth2 Resource Server.

## Responsibility

The service answers:

```txt
Was this notification request sent, pending, or failed?
```

It does not:

```txt
decide onboarding approval
own customer data
own account data
own identity links
authenticate users
```

## API

```txt
POST /internal/notifications/email
```

## Security

```txt
NOTIFICATION_WRITE -> send internal notifications
```

Swagger UI can authenticate against Keycloak using the `banking-swagger` client with Authorization Code and PKCE.

## Local email capture

Start Mailpit from the repository root:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

Open `http://localhost:8025` to inspect messages captured from
`notification-service` and Keycloak. Mailpit is local development
infrastructure and never sends those messages to Internet recipients.

## Template and link boundary

Each template declares the exact set of variables it accepts. A request with a
missing or extra variable fails before delivery, so callers cannot add
unreviewed PII or secrets to the stored payload. Templates also decide whether
their rendered content must be redacted from the audit record; this cannot be
downgraded by an API request. The legacy `sensitive` field may request more
redaction for compatibility, but it cannot disable the template policy.

`firstName` is restricted to a trimmed, single-line Unicode human name of at
most 80 code points. Expiration variables are canonical positive integers with
bounded ranges. Validation errors name only the contract field and never echo
the rejected value.

Templates that carry actions compare their URL against an explicitly configured
origin and exact path. Local frontend and identity origins are separate, and no
wildcard or hostname-suffix matching is allowed. URL validation does not fetch
the destination or follow redirects.

The onboarding credential invitation is owned by Keycloak. Keycloak generates
and signs the action link from `KEYCLOAK_PUBLIC_URL`, renders the banking email
theme, and sends the message through Mailpit locally. `notification-service`
does not reconstruct or validate that signed Keycloak URL in the normal
credential journey.

Sensitive variables and rendered bodies are never retained. Flyway migration
V4 also scrubs records written before template-owned redaction existed. A
SHA-256 fingerprint over recipient, template, and canonical ordered variables
allows exact idempotent replays without retaining the original payload.
Pre-V4 `SENT` records retain a null fingerprint because their original payload
cannot be reconstructed after scrubbing; they are validated and never resent.

Local SMTP has finite timeouts and no authentication. Authenticated deployed
SMTP fails at startup unless username/password are complete and transport uses
mandatory STARTTLS or implicit SSL.

## Database

```txt
notification_db.notification
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

