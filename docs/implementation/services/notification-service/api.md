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
    "magicLink": "http://localhost:4200/onboarding/continue?token=abc",
    "expiresInMinutes": "30"
  },
  "correlationId": "onboarding-application-id"
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

If delivery fails, the notification is persisted with status `FAILED` and the response includes `lastError`.

If a required template variable is missing, the service returns `400`.

