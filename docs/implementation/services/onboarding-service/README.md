# onboarding-service Implementation

`onboarding-service` is the process owner for digital account opening. It uses ports and adapters, MySQL persistence, scheduled workers with leases, optimistic locking, service discovery, and OAuth2 client credentials.

## Implemented Scope

- one-time magic links and recoverable opaque continuation access;
- transactional magic-link outbox, encrypted pending payload, and request cooldown;
- applicant data, DNI front/back references, and versioned terms;
- idempotent submit with `422 ONBOARDING_INCOMPLETE`;
- AUTO review with local and explicitly simulated controls;
- unique email/document reservations across concurrent workers;
- status history, review evidence, work items, and provisioning steps;
- customer KYC approval and `SAVINGS/ARS` account provisioning;
- provisional Keycloak user, customer-only realm role, and identity link;
- Keycloak `execute-actions-email` invitation for username/password setup;
- credential completion reconciliation, delayed account activation, and invitation resend cooldown;
- operational retry for technical review/provisioning failures;
- expiration of pre-provisioning applications and reservation release.

## Runtime

Configuration is served by `config-server` from `config-repository/onboarding-service.yaml`.

```powershell
cd onboarding-service
.\mvnw.cmd spring-boot:run
```

Default port and Swagger:

```txt
8087
http://localhost:8087/swagger-ui.html
```

## Security

Applicant-facing internal APIs require `ONBOARDING_READ` or `ONBOARDING_WRITE`. The BFF calls them with its confidential service account. Review and provisioning retry endpoints instead require `ONBOARDING_OPERATE`, which is not assigned to the BFF.

Outbound orchestration uses the dedicated confidential client `onboarding-orchestrator`. It obtains client-credentials tokens itself and never forwards the BFF or browser token. Customer creation/KYC approval, account creation/activation, and identity-link creation are restricted respectively by `CUSTOMER_PROVISION`, `ACCOUNT_PROVISION`, and `IDENTITY_PROVISION`; the orchestrator no longer needs the corresponding broad write roles.

`account-service` also replaces the orchestrator token before consulting `customer-service`, using its own service account with only `CUSTOMER_READ`.

## Durable Processing

Submit stores `SUBMITTED` and `AUTO_REVIEW` work in one transaction. Approval stores the provisioning work in the review completion transaction. Workers claim due work with a database lock and lease, then release the transaction before remote calls.

Retryable failures are timeouts, `429`, and `5xx`. Business validation, request-hash drift, identity conflicts, and unknown programming errors fail without destructive compensation. External errors are stored as sanitized codes.

See [API](api.md), [database](database.md), and [tests](tests.md).
