# notification-service API

## Send Email Notification

```txt
POST /internal/notifications/email
```

Request:

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

The template, not the caller, defines whether content is sensitive. It also
defines the complete variable set and any action-link policy. A caller cannot
downgrade redaction through this contract.

The legacy `sensitive` boolean is still accepted for compatibility and may ask
for additional redaction, but `false` never disables redaction required by the
template. New callers can omit it.

Response status:

```txt
201 Created
```

Response:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "channel": "EMAIL",
  "recipient": "person@example.com",
  "templateCode": "ONBOARDING_EMAIL_MAGIC_LINK",
  "correlationId": "onboarding-application-id",
  "subject": "Continua tu solicitud en Nerva Banking",
  "status": "SENT",
  "attemptCount": 1,
  "lastError": null,
  "sentAt": "2026-07-04T21:00:00Z",
  "createdAt": "2026-07-04T21:00:00Z",
  "updatedAt": "2026-07-04T21:00:00Z"
}
```

If delivery fails, the notification is persisted with status `FAILED`. `lastError` contains only a sanitized exception type.

Sensitive templates are rendered only for the SMTP call. Their stored variables
are empty and their text and HTML bodies are redacted. Repeating the same
request with the same `(templateCode, correlationId)` returns the existing
notification without sending another email. The service persists only a
one-way SHA-256 request fingerprint for this comparison. Reusing that key with a
different recipient or valid payload returns `409` for records created from V4
onward. Scrubbed pre-V4 `SENT` records retain a null fingerprint; their replay
is still validated and never sends another email.

If a required template variable is missing, or if the request contains a
variable not declared by that template, the service returns `400`.
Template values also have semantic contracts: customer names are trimmed,
single-line Unicode names up to 80 code points; minute expirations are `1..1440`
and hour expirations are `1..168`. Invalid values return `400` without being
reflected in the response.

Action-link templates enforce one configured exact origin and path per trust
boundary. In local development:

```txt
ONBOARDING_EMAIL_MAGIC_LINK
  origin: http://localhost:4200
  path:   /onboarding/continue

ONBOARDING_APPROVED_CREDENTIAL_INVITATION
  origin: http://localhost:8090
  path:   /realms/banking-ecosystem/login-actions/action-token
```

Origins contain only scheme, host, and effective port. Wildcards, hostname
suffix matches, user information, path prefixes, and destination redirects are
not accepted. A rejected URL is not included in the public error or logs.

The normal onboarding credential email does not call this API. Keycloak creates
and signs that action link itself, using `KEYCLOAK_PUBLIC_URL`, and sends the
message through its banking email theme. The identity template remains subject
to the strict policy if it is invoked independently.

Flyway migration V4 erases variables and rendered bodies from sensitive rows
created before this contract was introduced.

