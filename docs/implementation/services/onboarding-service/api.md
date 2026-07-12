# onboarding-service API

All endpoints are internal, protected, and rooted at `/internal/onboarding`. They are called by the BFF or operational tooling, never by the browser.

## Applicant Process

```txt
POST /applications
POST /magic-links/consume
POST /continuations/validate
POST /continuations/submissions
POST /continuations/credential-invitations/resend
GET  /applications/{applicationId}
```

`/continuations/submissions` is multipart and accepts one JSON submission plus `dniFront` and `dniBack`. The service uploads and verifies documents, stores applicant data and server-selected terms, transitions to `SUBMITTED`, and creates the AUTO review work item.

The operation is idempotent. Once `submittedAt` exists, a retry returns the current process state without revalidating terms, reuploading documents, or duplicating work.

Missing data, documents, or terms return:

```txt
422 ONBOARDING_INCOMPLETE
```

Invitation resend is valid only in `CREDENTIAL_SETUP_PENDING` and uses a persisted cooldown:

```txt
429 CREDENTIAL_INVITATION_COOLDOWN
```

## Operational Retry

```txt
POST /applications/{applicationId}/review/retry
POST /applications/{applicationId}/provisioning/retry
```

These protected internal endpoints accept only `REVIEW_FAILED` and `PROVISIONING_FAILED`. They are intentionally absent from the BFF.

## Token Handling

Continuation and magic-link secrets are supplied only to their specific internal commands. Databases store token hashes; the pending magic-link payload is encrypted for asynchronous delivery and erased after success, expiration, or terminal failure.

## Error Boundary

The BFF maps this internal API to stable public outcomes. Internal Problem Details, dependency errors, review evidence, and provisioning references are not relayed verbatim.
