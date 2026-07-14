# Mailpit local

Mailpit is the SMTP capture server for local customer journeys. It accepts
messages from `notification-service` and Keycloak, stores them in a local Docker
volume, and exposes a browser inbox without delivering mail to Internet
recipients.

## Boundaries

Mailpit is development infrastructure only. It is not a production mail relay,
does not decide when communications are sent, and does not own notification
delivery state. Those responsibilities remain in `notification-service` for
banking notifications and in Keycloak for identity action email.

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

`notification-service` connects through `localhost:1025`. Keycloak runs in a
container and uses the host-side Mailpit endpoint supplied by its Compose
configuration. Both defaults use empty credentials with authentication,
STARTTLS, and SSL disabled.

## Start

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

## Production SMTP

The normal local flow always targets Mailpit. An ignored local `.env` may copy
safe development defaults, but must not become a long-lived store for a real
SMTP username or password.

Deployed environments override every relevant SMTP value through runtime
environment variables or a secrets manager:

```txt
NOTIFICATION_SMTP_HOST
NOTIFICATION_SMTP_PORT
NOTIFICATION_SMTP_USERNAME
NOTIFICATION_SMTP_PASSWORD
NOTIFICATION_SMTP_FROM
NOTIFICATION_SMTP_AUTH
NOTIFICATION_SMTP_STARTTLS
NOTIFICATION_SMTP_STARTTLS_REQUIRED
NOTIFICATION_SMTP_SSL
NOTIFICATION_SMTP_CONNECTION_TIMEOUT_MS
NOTIFICATION_SMTP_READ_TIMEOUT_MS
NOTIFICATION_SMTP_WRITE_TIMEOUT_MS
```

Authenticated SMTP is accepted only with a complete username/password pair and
either mandatory STARTTLS or implicit SSL. STARTTLS and SSL cannot be enabled
together, and TLS verifies the SMTP server identity. Connection, read, and write
timeouts are finite and bounded; unsafe combinations fail during application
startup.

Keycloak has an equivalent set of host, port, sender, username, password,
authentication, STARTTLS, and SSL inputs in `infra/keycloak/.env.example`.
Those example values remain credential-free and point to Mailpit. A production
deployment injects the real values without copying them into an ignored file in
the repository workspace.

The repository contains no production provider or credential defaults.

SMTP configuration and browser-link trust are separate concerns. Local email
links use exact configured origins for the Angular frontend and Keycloak
identity endpoint; neither policy accepts wildcard hosts. Keycloak generates
its own signed action link from the fixed `KEYCLOAK_PUBLIC_URL` rather than
asking `notification-service` to assemble it.
