# Identity Linking

Identity linking connects authenticated external identities with internal banking customers.

## Purpose

Authentication and banking customer ownership are separate concerns.

Keycloak authenticates users and issues tokens.

`customer-service` owns banking customer data.

`identity-service` links both concepts:

```txt
provider + providerSubject -> customerId
```

## Current Providers

```txt
KEYCLOAK
GOOGLE
```

The current local runtime uses Keycloak as the token issuer.

The provider model allows the ecosystem to support additional identity providers later without changing customer ownership.

## Why Not Email

Email is not used as the primary identity key.

Reasons:

```txt
email can change
email can be reused
multiple providers can expose the same email
provider subject is the stable identifier inside that provider
```

## Current Boundary

`identity-service` does not register new customers.

For now, a customer must already exist in `customer-service`, and an identity link must point to that customer.
