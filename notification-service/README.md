# notification-service

`notification-service` owns banking notification requests and delivery state.

The first implemented channel is email through SMTP.

## Responsibility

Owns:

```txt
- notification request records
- notification templates
- email recipients
- SMTP delivery attempts
- delivery status and errors
```

Does not own:

```txt
- onboarding decisions
- customer data
- account data
- identity links
```

## Local Configuration

Service default port:

```txt
8083
```

Database defaults:

```txt
jdbc:mysql://localhost:3310/notification_db
notification_user
notification_password
```

SMTP is configured through environment variables:

```txt
NOTIFICATION_SMTP_HOST=localhost
NOTIFICATION_SMTP_PORT=1025
NOTIFICATION_SMTP_USERNAME=
NOTIFICATION_SMTP_PASSWORD=
NOTIFICATION_SMTP_FROM=no-reply@nerva.local
NOTIFICATION_SMTP_AUTH=false
NOTIFICATION_SMTP_STARTTLS=false
```

These defaults target the local Mailpit server. Start it from the repository
root before running an email journey:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

Captured messages are available at `http://localhost:8025`.

For local development, create a local `.env` file from `.env.example`:

```powershell
Copy-Item .env.example .env
```

The default file works with Mailpit and contains no credential. To use a real
SMTP provider, override host, port, username, password, sender,
`NOTIFICATION_SMTP_AUTH=true`, and the provider's STARTTLS setting. The real
`.env` file is ignored by Git. Do not commit real SMTP credentials.

The service still reads its shared configuration through Config Server. For local development it also imports the ignored `.env` file from its working directory using Spring Config Data. Operating-system environment variables keep their higher precedence, so deployed environments can inject secrets normally without a local file.

The config file in `config-repository/notification-service.yaml` contains safe
Mailpit defaults and environment-variable placeholders. SMTP secrets therefore
belong in `notification-service/.env`, process environment variables, or a
production secrets manager; never in `config-repository` or the Config Server
process.

## Run

Start infrastructure:

```powershell
docker compose -f ..\infra\mysql\docker-compose.yml up -d notification-mysql
```

Start from this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

Swagger:

```txt
http://localhost:8083/swagger-ui.html
```

## API

Send email notification:

```txt
POST /internal/notifications/email
```

Requires role:

```txt
NOTIFICATION_WRITE
```

Example body:

```json
{
  "recipient": "person@example.com",
  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
  "variables": {
    "magicLink": "http://localhost:4200/onboarding/continue#token=abc",
    "expiresInMinutes": "30"
  },
  "correlationId": "onboarding-delivery-id",
  "sensitive": true
}
```

Sensitive messages are rendered in memory for SMTP, while persisted variables and body are redacted. `(templateCode, correlationId)` is idempotent.

## Tests

```powershell
.\mvnw.cmd test
```
