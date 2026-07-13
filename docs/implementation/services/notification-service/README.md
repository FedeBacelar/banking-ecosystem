# notification-service

`notification-service` stores notification requests and sends templated email notifications.

## Current Status

Implemented.

Current capabilities:

- Stores email notification requests in `notification_db`.
- Renders known templates from `templateCode + variables`.
- Sends email through SMTP.
- Uses local Mailpit defaults (`localhost:1025`, without authentication or
  STARTTLS); deployed environments must inject their SMTP provider settings.
- Records delivery status, attempt count, sent timestamp, and last error.
- Registers with Eureka.
- Reads configuration from Config Server.
- Imports ignored local SMTP values from `notification-service/.env` when present; process environment variables have higher precedence.
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
`notification-service`. Mailpit is local development infrastructure and never
sends those messages to Internet recipients.

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

