# Home Banking BFF Session Security

`home-banking-bff` owns two distinct cookie-backed concerns. They must not be conflated.

## Authenticated OIDC Session

```txt
browser -> /web/auth/login/home
BFF -> /web/oauth2/authorization/keycloak
Keycloak -> /web/login/oauth2/code/keycloak
BFF -> server-side OAuth2 session
browser -> HttpOnly JSESSIONID
```

The public login endpoint represents one closed `HOME` journey. It accepts no destination input, the success handler always redirects to Angular `/app/inicio`, and the failure handler always redirects to `/error?reason=authentication`. Saved requests and browser-controlled return URLs cannot override those destinations.

`GET /web/me` requires this authenticated session. It resolves the Keycloak subject to the bank customer and resolves the customer and account aggregates with BFF machine credentials before returning only `{ username, displayName }`. It also materializes the readable CSRF cookie. An anonymous call receives `401 application/problem+json`; API calls never initiate interactive login.

`POST /web/logout` invalidates the local session and starts OIDC RP-initiated logout so the Keycloak session is also closed. Logout is a cookie-authorized mutation and remains CSRF protected. Angular submits a top-level form with `_csrf`; Keycloak returns to `/sesion-cerrada`, which is also the fallback when the provider does not advertise an end-session endpoint.

There is no public `/web/session` status endpoint. A frontend calls `/web/me` to read session state and navigates to `/web/auth/login/home` only after an explicit user action.

## Onboarding Continuation

`NB_ONBOARDING_CONTINUATION` is not an authenticated customer session. It is an opaque, HttpOnly capability scoped to `/web/onboarding` that identifies one applicant workflow after magic-link verification.

Properties:

```txt
HttpOnly=true
SameSite=Lax
Path=/web/onboarding
Secure=true outside local HTTP development
```

The BFF never returns the continuation token in JSON. `onboarding-service` stores only its hash.

## CSRF

The magic-link exchange materializes `NB-XSRF-TOKEN` in the same response that establishes continuation authority. Authenticated `/web/me` materializes the same cookie for the home-banking session. Angular reads this non-HttpOnly cookie and automatically sends `X-XSRF-TOKEN` on subsequent API mutations; a top-level logout form uses `_csrf`.

There is no `/web/csrf` endpoint and CSRF is not a business step. Application start and magic-link exchange are exempt because they do not trust an existing cookie; submit, invitation resend, and logout are protected.

## Browser Storage

Neither OIDC access tokens nor onboarding continuation tokens belong in `localStorage`, `sessionStorage`, URLs, or application state. The initial magic token uses a URL fragment and is removed immediately after the exchange.
