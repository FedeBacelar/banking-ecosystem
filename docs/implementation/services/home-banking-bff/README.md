# home-banking-bff Implementation

`home-banking-bff` is the only browser backend. It owns browser sessions, opaque onboarding cookies, CSRF enforcement, public DTOs, and response composition. It does not own onboarding state, customer/account lifecycle, documents, or credentials.

## Boundary

```txt
browser -> api-gateway:8085 /web/** -> home-banking-bff:8086 /web/**
```

Angular proxies to the gateway. Direct BFF access is diagnostics only; internal services are never browser routes.

## Minimal Onboarding API

```txt
POST /web/onboarding/applications
POST /web/onboarding/magic-links/consume
POST /web/onboarding/submissions
GET  /web/onboarding/status
POST /web/onboarding/credential-invitations/resend
```

The multipart submit is the capture boundary: the BFF forwards applicant data, terms acceptance, and both DNI files as one internal command. `onboarding-service` owns document upload, references, validation, review, and provisioning.

Application start is enumeration-safe. Status is presentation-oriented and never exposes review evidence, remote errors, external resource IDs, or Keycloak subjects.

## Authenticated Session API

```txt
GET  /web/auth/login/home
GET  /web/me
POST /web/logout
```

`/web/auth/login/home` is the only home login entry point and always starts the Keycloak authorization-code flow for the fixed `HOME` destination. The callback always returns to Angular `/app/inicio`; failures return to `/error?reason=authentication`. There is no browser-controlled `returnTo` parameter.

`/web/me` still verifies the Keycloak subject against `identity-service` and resolves the corresponding customer and accounts before returning. Its browser contract is intentionally limited to:

```json
{
  "username": "home-banking-user",
  "displayName": "Home Banking User"
}
```

An anonymous API request receives a `401` Problem Detail response and never a Keycloak redirect. Login is navigation initiated explicitly through the login endpoint.

`POST /web/logout` accepts a top-level form POST with the CSRF token in `_csrf`, invalidates the BFF session, initiates RP-initiated logout at Keycloak, and uses `/sesion-cerrada` as both its registered post-logout destination and safe fallback.

## Security

- Magic-link exchange sets `NB_ONBOARDING_CONTINUATION` as HttpOnly, `SameSite=Lax`, scoped to `/web/onboarding`.
- Magic-link exchange and authenticated `/web/me` materialize `NB-XSRF-TOKEN`; there is no `/web/csrf` request.
- Angular automatically sends `X-XSRF-TOKEN` on later same-origin mutations.
- Responses use `Cache-Control: no-store` and correlation IDs.
- Internal calls use dedicated client-credentials tokens selected by purpose; browser credentials are not propagated.

## Verification

```powershell
cd home-banking-bff
.\mvnw.cmd test
```
