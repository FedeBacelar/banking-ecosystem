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

The current implementation removes any incoming `Authorization` header before the Feign call and obtains a client-credentials token for the dedicated `account-service` machine client:

```txt
caller token -> account-service
                account-service token (CUSTOMER_READ) -> customer-service
```

The nested customer lookup is authenticated without propagating the caller's authority. In particular, an onboarding token carrying `ACCOUNT_PROVISION` is never forwarded to `customer-service`.

Initial account creation and activation require `ACCOUNT_PROVISION` for the inbound request. Customer validation is a separate authorization decision performed with the account service's own `CUSTOMER_READ` token.

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
