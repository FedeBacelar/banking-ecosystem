# Service Boundaries

Each service owns one business capability.

Service boundaries are based on business responsibility, not table names.

## Current Boundaries

## customer-service

Owns:

```txt
- Customer identity relationship with the bank.
- Natural person registration.
- Customer status.
- Initial KYC.
- Identification documents.
- Initial contact and address data.
```

Does not own:

```txt
- Accounts.
- Balances.
- Transactions.
```

## account-service

Owns:

```txt
- Bank accounts.
- Account identifiers.
- Account status.
- Account balance position.
- Account status history.
```

Does not own:

```txt
- Customer personal data.
- KYC.
- Transaction history.
- Exchange rates.
```

## Cross-Service Communication

Services should communicate through APIs or explicit integration mechanisms, not by querying each other's databases.

Current example:

```txt
account-service -> customer-service
```

This is used to validate that a customer exists and is eligible before opening an account.

## notification-service

Owns:

```txt
- Notification requests.
- Notification templates.
- Delivery channel state.
- Email delivery attempts.
- Provider delivery errors.
```

Does not own:

```txt
- Customer onboarding decisions.
- Customer data.
- Account data.
- Identity links.
- Authentication users.
```

Current example:

```txt
onboarding or another service -> notification-service
```

The caller decides that a notification should be sent. `notification-service` owns how that notification is rendered, delivered, and recorded.

## document-service

Owns:

```txt
- Document metadata.
- Document categories.
- Accepted file content type and size.
- Object storage keys and provider metadata.
- Storage status.
```

Does not own:

```txt
- Onboarding approval.
- Manual document review decisions.
- Customer data.
- Account data.
- Authentication users.
- Public document downloads.
```

Current example:

```txt
onboarding-service -> document-service
```

The caller decides that a document is required for a business process. `document-service` owns file acceptance, storage, and metadata.

## onboarding-service

Owns:

```txt
- Onboarding application state.
- Email verification and continuation tokens.
- Applicant workflow state transitions.
- Onboarding application expiration.
- Orchestration of onboarding steps.
```

Does not own:

```txt
- Final customer master data.
- Bank accounts.
- Document file storage.
- Notification delivery mechanics.
- Keycloak users or authentication sessions.
- Identity links.
```

Current examples:

```txt
onboarding-service -> notification-service
future onboarding-service -> document-service
future onboarding-service -> customer-service
future onboarding-service -> account-service
future onboarding-service -> identity-service
```

`onboarding-service` coordinates the applicant journey. Other services own the durable banking resources created after approval.

