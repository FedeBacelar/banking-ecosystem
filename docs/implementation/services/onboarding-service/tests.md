# onboarding-service Tests

```powershell
cd onboarding-service
.\mvnw.cmd test
```

The current suite covers:

- token lifecycle, post-submit session recovery, and state history;
- complete, incomplete, and repeated submit;
- AUTO approval/rejection and auditable simulations;
- technical retry state transitions;
- resume from the first incomplete provisioning step;
- failed credential invitation recovery;
- Keycloak conflict reconciliation and credential completion;
- MySQL migrations and application context;
- concurrent email/document reservation;
- concurrent work claiming by two worker threads.

The MySQL integration tests use Testcontainers. A clean full run currently executes 42 onboarding tests with no failures.

## Fresh-Volume E2E

The release-level local verification starts MySQL, MinIO and Keycloak without existing banking volumes, then starts Config Server, Eureka, all business services, the BFF, Gateway and Angular.

The happy path must demonstrate:

```txt
CSRF -> generic 202 application start -> SMTP magic link -> continuation cookie
     -> applicant data -> DNI front/back -> terms -> idempotent submit
     -> AUTO review -> durable provisioning -> Keycloak invitation
     -> credential reconciliation -> COMPLETED -> BFF home-banking login
```

Verify that browser calls use only `http://localhost:8085/web/**`, local and simulated checks keep their provenance, customer/account/identity resources are not duplicated, and the final BFF session derives the customer from the Keycloak subject.

Also verify an incomplete submit (`422 ONBOARDING_INCOMPLETE`), post-submit session recovery, and credential invitation cooldown (`429 CREDENTIAL_INVITATION_COOLDOWN`).

## Verified Local Run

A fresh-volume run completed the public flow exclusively through Gateway and
the BFF. All seven review checks retained their `LOCAL` or `SIMULATED`
provenance, all seven provisioning steps succeeded once, and the resulting
customer, `SAVINGS/ARS` account, identity link, and Keycloak user were
consistent across service-owned databases.

The run also verified generic application start, real SMTP delivery, CSRF,
HttpOnly continuation, repeated submit, invitation cooldown, credential
reconciliation to `COMPLETED`, and an Authorization Code login whose customer
was resolved from the authenticated Keycloak subject. The automated run used
the Keycloak Admin API to simulate the human username/password actions; the
emailed Keycloak action-page UX remains a manual browser check.
