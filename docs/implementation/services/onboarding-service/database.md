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
onboarding_magic_link_delivery
onboarding_email_request_guard
onboarding_credential_invitation_delivery
```

`onboarding_application` stores the current state, review mode and policy snapshot, submit/decision timestamps, secret hashes, expirations, and optimistic-lock version.

`onboarding_status_history` is the append-only transition audit with actor type and reason code.

`onboarding_review_check` separates execution status from business outcome and records mode/provider/policy. Simulated approvals remain explicitly marked `SIMULATED` and `SIMULATOR`.

`onboarding_work_item` is the durable queue for magic-link delivery, AUTO review, provisioning, and credential reconciliation. Due work is claimed under a database lock with a lease; optimistic versioning fences a stale lease owner.

`onboarding_provisioning_step` records request hash, external reference, attempts, retry time, sanitized error, and timestamps. A retry with a changed request hash is rejected, and successful steps are skipped.
The pair `(step_type, external_reference)` is unique when an external reference is present, preventing the same provisioned resource from being assigned to multiple steps.

`onboarding_uniqueness_reservation` serializes normalized email/document ownership across applications. Reservations are released on functional termination and converted after provisioning.

`onboarding_magic_link_delivery` is the transactional outbox for email access links. Only encrypted pending payload is stored, and the ciphertext is discarded after terminal handling.

`onboarding_email_request_guard` serializes application starts per normalized email and enforces cooldown without exposing account existence.

`onboarding_credential_invitation_delivery` is the durable delivery queue for credential invitations. It stores only a hash of the idempotency key and tracks status, attempts, retry time, lease, sanitized error, and delivery time. A unique `(application_id, idempotency_key_hash)` constraint makes repeated requests safe.

Document rows contain document-service UUIDs only; bytes remain under document-service/MinIO ownership.

Flyway migrations `V1` through `V10` create the current schema. The editable ecosystem model is [schema.dbml](../../../database/schema.dbml).
