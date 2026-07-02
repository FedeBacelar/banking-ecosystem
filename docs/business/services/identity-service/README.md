# identity-service

`identity-service` owns the link between an authenticated external identity and a banking customer.

## Business Purpose

A login identity is not the same as a bank customer.

Keycloak can authenticate a person, but the banking ecosystem still needs to know which internal customer that identity belongs to.

`identity-service` answers:

```txt
Which customer is linked to this authenticated identity?
```

## Current Boundary

The service does not:

```txt
authenticate users
store passwords
create customers
perform onboarding
perform KYC
```

Those responsibilities remain outside this service.

## Current Link Model

```txt
provider + providerSubject -> customerId
```

`customerId` is owned by `customer-service`.

## Current Statuses

```txt
PENDING_VERIFICATION
ACTIVE
DISABLED
```

Only `ACTIVE` links should allow a logged-in identity to be resolved to a banking customer.
