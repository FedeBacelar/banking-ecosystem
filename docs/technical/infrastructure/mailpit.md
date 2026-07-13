# Mailpit local

Mailpit is the SMTP capture server for local customer journeys. It accepts
messages from `notification-service`, stores them in a local Docker volume, and
exposes a browser inbox without delivering mail to Internet recipients.

## Boundaries

Mailpit is development infrastructure only. It is not a production mail relay,
does not decide when communications are sent, and does not own notification
delivery state. Those responsibilities remain in `notification-service`.

## Defaults

```txt
SMTP: localhost:1025
Web UI and API: http://localhost:8025
Authentication: disabled
STARTTLS: disabled
Host exposure: 127.0.0.1 only
```

The container uses the pinned `axllent/mailpit:v1.30.0` image and reports
readiness through its built-in `readyz` command. Messages persist in the
`mailpit_data` Docker volume until it is explicitly removed.

## Start

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

## Production SMTP

Deployed environments override every relevant SMTP value through environment
variables or a secrets manager:

```txt
NOTIFICATION_SMTP_HOST
NOTIFICATION_SMTP_PORT
NOTIFICATION_SMTP_USERNAME
NOTIFICATION_SMTP_PASSWORD
NOTIFICATION_SMTP_FROM
NOTIFICATION_SMTP_AUTH
NOTIFICATION_SMTP_STARTTLS
```

The repository contains no production provider or credential defaults.
