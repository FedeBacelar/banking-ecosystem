# account-service Customer Integration

`account-service` validates customers through `customer-service` before opening an account.

## Current Mechanism

Implemented with Feign:

```txt
CustomerFeignClient
```

Current endpoint consumed:

```http
GET /customers/{customerId}
```

## Security

The current implementation forwards the incoming `Authorization` header to the Feign call:

```txt
client -> api-gateway -> account-service -> customer-service
```

This keeps the customer lookup authenticated after business services started validating JWT access tokens directly.

For the current local setup, account opening requires a token that can write accounts and read the referenced customer. The local `api-tester` user satisfies this because it has all current API roles.

A production-grade evolution would use service-to-service credentials or token exchange for this internal customer lookup.

## Why It Exists

An account should not be opened for a customer that does not exist or is not eligible.

Today, the account service needs only a small customer view:

- Customer id.
- Customer number.
- Customer status.
- Customer display data needed by the use case response.

## Current Eligibility Rule

The customer must exist and must be in an allowed status for account opening.

Customer statuses known by account-service:

```txt
PENDING_KYC
ACTIVE
SUSPENDED
CLOSED
```

## Important Boundary

`account-service` does not own customer personal data.

The `customer_id` stored in the account database is a reference to the customer service aggregate. It is not a local customer table.

## Current Limitation

The integration is synchronous.

If `customer-service` is unavailable, account opening cannot validate the customer and should fail.
