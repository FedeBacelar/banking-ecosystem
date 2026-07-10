# home-banking-bff Implementation

`home-banking-bff` is the only browser backend. It owns browser sessions, HttpOnly onboarding cookies, CSRF, public DTOs, and composition; it does not own banking lifecycle or persistence.

## Access Model

```txt
browser -> api-gateway:8085 /web/** -> home-banking-bff:8086 /web/**
```

Direct port `8086` is diagnostics only. Angular proxies `/web` to the gateway, never to a business service.

## Public Onboarding Contracts

```txt
GET    /web/csrf
POST   /web/onboarding/applications
POST   /web/onboarding/magic-links/consume
GET    /web/onboarding/session
DELETE /web/onboarding/session
PUT    /web/onboarding/applicant-data
POST   /web/onboarding/documents/{category}
PUT    /web/onboarding/terms
POST   /web/onboarding/submissions
GET    /web/onboarding/status
POST   /web/onboarding/credential-invitations/resend
```

Application start always returns generic `202 Accepted`; it does not reveal whether an email is already known. Status exposes only `applicationId`, public state, `nextAction`, and `updatedAt`. It never exposes review evidence, external resource IDs, Keycloak subject, or dependency errors.

## Security

- Mutations require the `NB-XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header initialized by `/web/csrf`. The cookie uses `Path=/` so the SPA can read it while its routes live outside `/web`.
- The continuation token is an HttpOnly, `SameSite=Lax` cookie scoped to `/web/onboarding`.
- Onboarding responses use `Cache-Control: no-store` and `X-Correlation-Id`.
- Downstream codes are allowlisted; authorization and remote implementation details become a generic public service error.
- The BFF calls onboarding/document/notification with its service-account token. Browser tokens are not forwarded for anonymous onboarding.

After normal home-banking login, the BFF keeps the OAuth2 authorized client server-side and resolves the authenticated identity to a customer.

## Verification

```powershell
cd home-banking-bff
.\mvnw.cmd test
```

The current suite has 43 passing tests, including context-path routing, CSRF, safe public errors, cookies, and new submit/status/resend contracts.
