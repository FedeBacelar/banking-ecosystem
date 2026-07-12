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
    "magicLink": "http://localhost:4200/onboarding/continue#token=abc",
    "expiresInMinutes": "30"
  },
  "correlationId": "onboarding-delivery-id",
  "sensitive": true
}
```

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

For `sensitive=true`, templates are rendered only for the SMTP call. Stored variables are empty and the body is redacted. Repeating the same `(templateCode, correlationId)` returns the existing notification without sending another email.

If a required template variable is missing, the service returns `400`.

