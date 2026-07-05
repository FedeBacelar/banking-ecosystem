# onboarding-service API

All current endpoints are internal and protected.

Base path:

```txt
/internal/onboarding
```

## Start Application

```txt
POST /internal/onboarding/applications
```

Request:

```json
{
  "email": "applicant@example.com"
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
  "email": "applicant@example.com",
  "status": "EMAIL_VERIFICATION_PENDING",
  "magicLinkExpiresAt": "2026-07-05T20:30:00Z",
  "emailVerifiedAt": null,
  "continuationExpiresAt": null,
  "expiresAt": "2026-07-20T20:00:00Z",
  "createdAt": "2026-07-05T20:00:00Z",
  "updatedAt": "2026-07-05T20:00:00Z"
}
```

## Consume Magic Link

```txt
POST /internal/onboarding/magic-links/consume
```

Request:

```json
{
  "token": "opaque-token-from-email"
}
```

Response:

```json
{
  "applicationId": "11111111-1111-1111-1111-111111111111",
  "status": "IN_PROGRESS",
  "continuationToken": "opaque-continuation-token",
  "continuationExpiresAt": "2026-07-05T22:00:00Z"
}
```

The continuation token is returned only to the caller. It is stored server-side as a hash.

## Validate Continuation

```txt
POST /internal/onboarding/continuations/validate
```

Request:

```json
{
  "token": "opaque-continuation-token"
}
```

Response:

```json
{
  "applicationId": "11111111-1111-1111-1111-111111111111",
  "status": "IN_PROGRESS",
  "continuationToken": null,
  "continuationExpiresAt": "2026-07-05T22:00:00Z"
}
```

## Get Application

```txt
GET /internal/onboarding/applications/{applicationId}
```

Response status:

```txt
200 OK
```

## Errors

```txt
400 Invalid request or token
404 Application not found
409 Duplicate active application, consumed token, invalid state, or concurrent update
410 Expired magic link or continuation token
503 Notification delivery unavailable
```
