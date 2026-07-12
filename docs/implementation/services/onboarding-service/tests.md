# onboarding-service Tests

```powershell
cd onboarding-service
.\mvnw.cmd test
```

The suite covers:

- one-time magic links, encrypted delivery outbox, retry, expiration, and payload disposal;
- continuation recovery without a public session endpoint;
- composite, incomplete, repeated, and concurrent submit behavior;
- versioned AUTO review with local and explicitly simulated evidence;
- functional rejection versus technical review failure;
- concurrent email/document uniqueness reservations;
- durable provisioning resume and request-hash invariants;
- Keycloak conflict reconciliation and credential invitation;
- credential waiting, expiration, technical failure, and final account activation;
- one work-item claim across two workers;
- optimistic fencing after a lease expires;
- all Flyway migrations against MySQL Testcontainers.

## Fresh-Volume E2E

After automated suites, the release-level manual check starts MySQL, MinIO, and Keycloak from empty banking volumes, followed by Config Server, Eureka, internal services, BFF, Gateway, and Angular.

Expected happy path:

```txt
generic 202 start -> SMTP magic link -> continuation/XSRF cookies
  -> one multipart submission -> AUTO review -> durable provisioning
  -> Keycloak action email -> username/password setup
  -> credential reconciliation -> account activation -> COMPLETED
  -> OIDC login -> /web/me resolves customer from Keycloak subject
```

Browser network traffic must remain under `http://localhost:8085/web/**`. There must be no explicit CSRF/session bootstrap, direct service route, duplicate customer/account/identity, leaked token in query parameters, or sensitive dependency detail in public errors.

Also verify repeated submit, invitation cooldown, a recovered continuation after submit, and that simulated checks remain visibly simulated in service-owned audit data.
