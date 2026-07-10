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
  "email": "applicant@example.com",
  "status": "IN_PROGRESS",
  "continuationExpiresAt": "2026-07-05T22:00:00Z"
}
```

## Save Applicant Data

```txt
PUT /internal/onboarding/continuations/applicant-data
```

Stores the first structured applicant data step for an `IN_PROGRESS` onboarding application. The continuation token identifies and authorizes the applicant journey.

Request:

```json
{
  "continuationToken": "opaque-continuation-token",
  "firstName": "Federico",
  "middleName": null,
  "lastName": "Bacelar",
  "birthDate": "1990-05-10",
  "nationality": "AR",
  "documentType": "DNI",
  "documentNumber": "12345678",
  "documentIssuingCountry": "AR",
  "documentExpirationDate": null,
  "phoneNumber": "+5491122223333",
  "street": "Av Siempre Viva",
  "streetNumber": "742",
  "city": "Buenos Aires",
  "province": "Buenos Aires",
  "postalCode": "1000",
  "country": "AR"
}
```

Response:

```json
{
  "applicationId": "11111111-1111-1111-1111-111111111111",
  "firstName": "Federico",
  "lastName": "Bacelar",
  "documentType": "DNI",
  "documentNumber": "12345678",
  "country": "AR",
  "createdAt": "2026-07-05T20:00:00Z",
  "updatedAt": "2026-07-05T20:00:00Z"
}
```

## Save Document Reference

```txt
PUT /internal/onboarding/continuations/documents/{category}
```

Stores or replaces the document reference for an `IN_PROGRESS` onboarding application. The file itself is owned by `document-service`; onboarding stores only the `documentId` returned by that service.

Supported onboarding categories:

```txt
DNI_FRONT
DNI_BACK
```

Request:

```json
{
  "continuationToken": "opaque-continuation-token",
  "documentId": "22222222-2222-2222-2222-222222222222"
}
```

Response:

```json
{
  "id": "33333333-3333-3333-3333-333333333333",
  "applicationId": "11111111-1111-1111-1111-111111111111",
  "category": "DNI_FRONT",
  "documentId": "22222222-2222-2222-2222-222222222222",
  "createdAt": "2026-07-05T20:00:00Z",
  "updatedAt": "2026-07-05T20:00:00Z"
}
```

Saving the same category again replaces the document reference and keeps the operation idempotent from the onboarding perspective.

## Accept Terms

```txt
PUT /internal/onboarding/continuations/terms
```

Captures terms acceptance for an `IN_PROGRESS` onboarding application.

Request:

```json
{
  "continuationToken": "opaque-continuation-token",
  "accepted": true,
  "termsVersion": "ONBOARDING_TERMS_AR_V1"
}
```

Response:

```json
{
  "applicationId": "11111111-1111-1111-1111-111111111111",
  "termsVersion": "ONBOARDING_TERMS_AR_V1",
  "acceptedAt": "2026-07-05T20:00:00Z",
  "createdAt": "2026-07-05T20:00:00Z",
  "updatedAt": "2026-07-05T20:00:00Z"
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
