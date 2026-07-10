# onboarding-service Database

Database: `onboarding_db`.

## Tables

```txt
onboarding_application
onboarding_applicant_data
onboarding_document_reference
onboarding_terms_acceptance
onboarding_status_history
onboarding_review_check
onboarding_work_item
onboarding_provisioning_step
onboarding_uniqueness_reservation
```

`onboarding_application` stores current state, `review_mode`, policy version, submit/decision timestamps, token hashes, expirations, and optimistic-lock version.

`onboarding_status_history` is the append-only audit of every state transition, including actor type and reason code.

`onboarding_review_check` keeps one result per application/control with separate execution mode, execution status, business outcome, provider, policy, and attempts. Simulations are persisted as simulations rather than indistinguishable approvals.

`onboarding_work_item` is the durable queue for `AUTO_REVIEW`, `PROVISIONING`, and `CREDENTIAL_RECONCILIATION`. Its unique application/job constraint prevents duplicate work. Due work uses `next_attempt_at`, lease timestamps, and optimistic locking.

`onboarding_provisioning_step` records each downstream step, request hash, external reference, attempts, next retry, sanitized error, and timestamps. Successful steps are skipped on retry.

`onboarding_uniqueness_reservation` has a unique `(reservation_type, normalized_value)` key. It serializes email/document ownership across concurrent application workers. Reservations become `RELEASED` on rejection/expiration and `CONVERTED` on completion.

The document table stores only document-service UUIDs. File bytes remain in MinIO under document-service ownership. Raw magic-link and continuation tokens are never stored.

Flyway migrations `V1` through `V6` create the current schema. The editable ecosystem model is [schema.dbml](../../../database/schema.dbml).
