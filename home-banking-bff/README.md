# home-banking-bff

`home-banking-bff` is the only browser backend. It stays behind `api-gateway` and owns browser-specific security and contracts, not banking lifecycle or persistence.

## Responsibilities

- Start and complete OAuth2/OIDC login with Keycloak.
- Keep the authenticated OAuth2 state server-side in an HttpOnly session.
- Resolve the authenticated Keycloak subject through `identity-service`.
- Compose customer-facing responses without accepting a browser-selected `customerId`.
- Keep the onboarding continuation token in an HttpOnly cookie.
- Protect cookie-authorized mutations with CSRF.
- Translate the small public onboarding API into internal `onboarding-service` calls.
- Obtain dedicated client-credentials tokens for internal calls; browser tokens are not forwarded.

## Public Topology

```txt
browser -> api-gateway:8085 /web/** -> home-banking-bff:8086 /web/**
```

Port `8086` is available only for technical diagnostics. Frontend traffic must use the gateway.

## Public Contracts

Authenticated home banking:

```txt
GET  /web/me
POST /web/logout
```

Digital onboarding:

```txt
POST /web/onboarding/applications
POST /web/onboarding/magic-links/consume
POST /web/onboarding/submissions
GET  /web/onboarding/status
POST /web/onboarding/credential-invitations/resend
```

There is no public CSRF bootstrap endpoint and no onboarding session endpoint.

- Application start returns generic `202 Accepted` and does not reveal whether the email exists.
- Magic-link exchange sets the opaque continuation cookie and materializes the readable XSRF cookie in the same response.
- Submit is one multipart command containing applicant data, terms acceptance, DNI front, and DNI back.
- Status exposes only `applicationId`, public status, `nextAction`, and `updatedAt`.
- Internal checks, dependency errors, Keycloak subjects, and provisioning references never reach the browser.

## CSRF Model

Application start and magic-link exchange do not rely on an existing browser cookie and are exempt from CSRF. Once the exchange establishes continuation authority, Spring Security requires the `X-XSRF-TOKEN` header on later mutations.

Angular reads `NB-XSRF-TOKEN` and sends the header automatically. CSRF is a transport defense, not a frontend workflow step and not an extra request.

## Internal Access

The BFF calls internal services through Eureka. It uses purpose-specific confidential clients and least-privilege roles. The onboarding process itself remains owned by `onboarding-service`; the BFF does not orchestrate review or provisioning.

## Verification

```powershell
.\mvnw.cmd test
```

Tests cover the real `/web` context path, cookies, CSRF, safe public errors, internal token purposes, and the minimal onboarding contract.
