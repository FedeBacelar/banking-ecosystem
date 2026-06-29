# Customer Service Business View

`customer-service` represents the customer's formal relationship with the bank.

It is not just a customer table. It models the business idea of a party becoming a bank customer.

## Owns

```txt
- Identifiable parties.
- Natural person customers.
- Identification documents.
- Initial contact data.
- Initial address data.
- Customer operational status.
- Initial KYC profile.
- Customer status history.
```

## Does Not Own

```txt
- Bank accounts.
- Balances.
- Transactions.
- Cards.
- Loans.
- Authentication users.
- Authorization rules.
```

## Current Scope

The current implementation supports natural person customers.

## Related Documents

```txt
lifecycle.md
kyc.md
```
