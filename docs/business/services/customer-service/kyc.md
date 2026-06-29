# Customer KYC

KYC means Know Your Customer.

In banking, KYC is the process of validating that the bank knows who the customer is and whether the customer is acceptable from a compliance and risk perspective.

## Current Model

The current implementation creates a basic KYC profile when a natural person customer is registered.

Initial values:

```txt
CustomerStatus: PENDING_KYC
KycStatus: PENDING_REVIEW
RiskLevel: LOW
```

## Approval

Approving KYC changes the customer to:

```txt
CustomerStatus: ACTIVE
KycStatus: APPROVED
```

## Rejection

Rejecting KYC closes the customer relationship:

```txt
CustomerStatus: CLOSED
KycStatus: REJECTED
```
