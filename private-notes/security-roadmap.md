# Security roadmap notes

## Resource ownership authorization

Future hardening: business services should validate ownership, not only roles.

Current BFF flow derives the customer from:

```txt
Keycloak subject -> identity-service -> customerId
```

That protects the browser flow from sending an arbitrary `customerId`.

Still, `customer-service` and `account-service` should eventually validate that the authenticated customer can access the requested resource.

Example:

```txt
account.customerId == authenticatedCustomerId
```

If it does not match, return `403 Forbidden` or `404 Not Found` depending on the desired information-disclosure policy.

Evaluate options later:

```txt
1. customer_id claim in token
2. service lookup to identity-service
3. trusted internal headers from BFF/gateway
4. token exchange / enriched internal token
```
