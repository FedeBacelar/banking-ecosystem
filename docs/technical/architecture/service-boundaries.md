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

