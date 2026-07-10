# onboarding-service API

All endpoints are internal, protected, and rooted at `/internal/onboarding`. They are not browser contracts.

## Applicant Flow

```txt
POST /applications
POST /magic-links/consume
POST /continuations/validate
PUT  /continuations/applicant-data
PUT  /continuations/documents/{DNI_FRONT|DNI_BACK}
PUT  /continuations/terms
POST /continuations/submissions
POST /continuations/credential-invitations/resend
GET  /applications/{applicationId}
```

Continuation mutations accept the opaque continuation token in the internal request DTO. Only its SHA-256 hash is persisted.

Submit responds `202 Accepted`. It is idempotent: repeating it returns the current state and does not duplicate checks or work. Missing applicant data, either DNI side, or the configured terms version returns:

```txt
422 ONBOARDING_INCOMPLETE
```

Credential invitation resend is available only in `CREDENTIAL_SETUP_PENDING` and enforces a persisted cooldown:

```txt
429 CREDENTIAL_INVITATION_COOLDOWN
```

## Operational Retry

```txt
POST /applications/{applicationId}/review/retry
POST /applications/{applicationId}/provisioning/retry
```

These endpoints require `ONBOARDING_WRITE`, are not exposed by the BFF, and only accept `REVIEW_FAILED` or `PROVISIONING_FAILED` respectively.

## Error Semantics

```txt
400 invalid request or token
404 application not found
409 invalid transition, consumed link, or concurrent update
410 expired magic link/continuation
422 incomplete application
429 invitation cooldown
503 notification or technical dependency unavailable
```

The BFF does not relay this internal Problem Detail verbatim. It allowlists safe codes and returns a smaller public contract.
