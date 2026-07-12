# onboarding-service

`onboarding-service` owns the digital onboarding process for a person who does not yet have a banking user, customer, account, or identity link.

## Capability

The service controls:

- email ownership verification and continuation sessions;
- applicant data, required document references, and terms acceptance;
- submit integrity and the onboarding state machine;
- automated review decisions and their evidence;
- durable provisioning of customer, account, Keycloak user, and identity link;
- credential setup invitation, reconciliation, retry, and expiration.

It does not own customer master data, accounts, file content, email delivery mechanics, Keycloak credentials, or identity links. Those resources remain owned by their respective services.

## Journey

```txt
EMAIL_VERIFICATION_PENDING -> IN_PROGRESS -> SUBMITTED
SUBMITTED -> UNDER_AUTOMATED_REVIEW
UNDER_AUTOMATED_REVIEW -> APPROVED | REJECTED | REVIEW_FAILED
APPROVED -> PROVISIONING
PROVISIONING -> CREDENTIAL_SETUP_PENDING | PROVISIONING_FAILED
CREDENTIAL_SETUP_PENDING -> COMPLETED | CREDENTIAL_SETUP_EXPIRED | CREDENTIAL_SETUP_FAILED
EMAIL_VERIFICATION_PENDING | IN_PROGRESS | SUBMITTED | UNDER_AUTOMATED_REVIEW -> EXPIRED
```

`REVIEW_FAILED`, `PROVISIONING_FAILED`, and `CREDENTIAL_SETUP_FAILED` represent technical failures, not commercial rejection. Review and provisioning failures have protected operational retry endpoints. Credential setup expiration and failure are terminal in the current applicant-facing contract.

## Review Model

Review mode is modeled as `AUTO | MANUAL`; the application refuses to start with `MANUAL` until a real backoffice exists.

Every control stores execution mode, execution state, outcome, policy version, provider, reason code, attempts, and timestamps. Local controls can reject. Document proofing, sanctions/PEP, and fraud are currently explicit `SIMULATED` approvals with provider `SIMULATOR` and code `SIMULATED_APPROVAL`.

## Provisioning Principle

Provisioning is a durable process manager in MySQL. It retries from the first incomplete step and never deletes a customer, account, identity, or Keycloak user as automatic compensation. Downstream creates use idempotency keys derived from the application and step.

## Public Boundary

The browser never calls this service directly. Browser traffic follows:

```txt
banking-web -> api-gateway /web -> home-banking-bff -> onboarding-service
```

The BFF owns HttpOnly continuation cookies, CSRF, safe public contracts, and error sanitization.
