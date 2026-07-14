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
NOTIFICATION_SMTP_STARTTLS_REQUIRED=false
NOTIFICATION_SMTP_SSL=false
NOTIFICATION_SMTP_CONNECTION_TIMEOUT_MS=5000
NOTIFICATION_SMTP_READ_TIMEOUT_MS=10000
NOTIFICATION_SMTP_WRITE_TIMEOUT_MS=10000
```

These defaults target the local Mailpit server. Start it from the repository
root before running an email journey:

```powershell
docker compose -f infra/mailpit/docker-compose.yml up -d
```

Captured messages are available at `http://localhost:8025`.

For local development, an optional local `.env` can be created from
`.env.example`:

```powershell
Copy-Item .env.example .env
```

The example works with Mailpit and contains no credential. The normal local
journey must preserve those settings: ignored `.env` files are not a storage
location for real SMTP usernames or passwords. A deployed environment supplies
its provider host, port, credentials, sender, authentication, and STARTTLS
settings through runtime variables or a secrets manager.

When authentication is enabled, username and password must both be present and
the connection must use mandatory STARTTLS or implicit SSL. STARTTLS and SSL are
mutually exclusive, TLS verifies the server identity, and every SMTP operation
has a timeout between 1 and 60 seconds. Invalid combinations stop the service at
startup without echoing their values.

The service still reads its shared configuration through Config Server. For local development it also imports the ignored `.env` file from its working directory using Spring Config Data. Operating-system environment variables keep their higher precedence, so deployed environments can inject secrets normally without a local file.

The config file in `config-repository/notification-service.yaml` contains safe
Mailpit defaults and environment-variable placeholders. Production SMTP
secrets belong in runtime environment variables or a secrets manager; never in
`notification-service/.env`, `config-repository`, or the Config Server process.

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
    "magicLink": "http://localhost:4200/onboarding/continue#token=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
    "expiresInMinutes": "30"
  },
  "correlationId": "onboarding-delivery-id"
}
```

The selected template owns its variable schema, action-link policy, and audit
sensitivity. The caller cannot make a sensitive template persist its content by
changing a request flag. Sensitive templates are rendered in memory for SMTP,
while persisted variables and bodies are redacted. Missing or additional
variables are rejected instead of being silently stored.

Customer names are single-line human names of at most 80 characters. Expiration
values are canonical positive integers with bounded minute/hour ranges. This
prevents a compromised caller from turning trusted branded templates into
arbitrary copy.

The legacy `sensitive` request field remains compatible: `true` may request
additional redaction, while `false` can never disable redaction required by the
template.

Action links must match the exact origin and path configured for their
template. Local onboarding email accepts only the Angular frontend origin, and
identity email accepts only the Keycloak identity origin. These checks do not
use wildcards, hostname suffixes, or redirects.

Credential setup email is not assembled by this service in the onboarding
journey. Keycloak creates and signs its action link, applies the banking email
theme, and sends it through the same local Mailpit server. `(templateCode,
correlationId)` remains the idempotency boundary for notifications owned by
this service. For records created from V4 onward, exact replays return the
existing result without another email; reusing the key with another recipient
or payload returns `409`. A one-way SHA-256 request fingerprint supports this
comparison while sensitive variables and bodies remain redacted. Migration V4
also scrubs older rows; legacy `SENT` rows keep a null fingerprint, are still
fully validated, and can only return the existing result without resending.

## Tests

```powershell
.\mvnw.cmd test
```
