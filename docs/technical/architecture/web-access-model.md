# Web Access Model

Browser traffic always follows one boundary:

```txt
browser -> api-gateway /web/** -> home-banking-bff
```

The browser never discovers or calls internal services.

## Anonymous Onboarding

An applicant initially has no Keycloak user or banking customer.

```txt
email start
  -> magic-link exchange
  -> opaque continuation cookie
  -> composite submission
  -> asynchronous status
  -> Keycloak credential setup
```

The BFF stores the continuation token in an HttpOnly cookie and calls `onboarding-service` with its own client-credentials token. The browser receives only public status and `nextAction`.

Application start and magic-link exchange are not authorized by ambient cookies. The exchange creates both continuation authority and the XSRF cookie. Later mutations require the automatic XSRF header; no CSRF bootstrap request exists.

The composite submit contains applicant data, terms acceptance, DNI front, and DNI back. `onboarding-service`, not the frontend or BFF, coordinates document storage, review, provisioning, and credential readiness with its purpose-specific provisioning roles. If `account-service` must validate the new customer, it uses its own `CUSTOMER_READ` machine token instead of forwarding the orchestrator token.

## Authenticated Home Banking

After credential setup, Keycloak authenticates the customer and the BFF owns the server-side OIDC session.

```txt
Keycloak subject
  -> identity-service
  -> customerId
  -> customer-service and account-service
```

The browser does not provide `customerId`. The BFF derives it from the authenticated subject and obtains a dedicated machine token for internal reads. The user's OIDC token stays inside the BFF session and is not forwarded through the service graph.

## Contract Boundary

Public contracts are designed for screens and actions. Internal contracts are designed for service ownership. Public responses never expose review checks, provisioning references, dependency failures, or administrative identity data.
