# Home Banking BFF Session Security

`home-banking-bff` owns two distinct cookie-backed concerns. They must not be conflated.

## Authenticated OIDC Session

```txt
browser -> /web/oauth2/authorization/keycloak
Keycloak -> /web/login/oauth2/code/keycloak
BFF -> server-side OAuth2 session
browser -> HttpOnly JSESSIONID
```

`GET /web/me` requires this authenticated session. It resolves the Keycloak subject to the bank customer and composes the home context with BFF machine credentials.

`POST /web/logout` invalidates the local session and starts OIDC logout so the Keycloak session is also closed. Logout is a cookie-authorized mutation and remains CSRF protected.

There is no public `/web/session` status endpoint. A frontend that needs the current authenticated context calls `/web/me` and handles authentication through the normal OIDC entry point.

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

The magic-link exchange materializes `NB-XSRF-TOKEN` in the same response that establishes continuation authority. Angular reads this non-HttpOnly cookie and automatically sends `X-XSRF-TOKEN` on subsequent mutations.

There is no `/web/csrf` endpoint and CSRF is not a business step. Application start and magic-link exchange are exempt because they do not trust an existing cookie; submit, invitation resend, and logout are protected.

## Browser Storage

Neither OIDC access tokens nor onboarding continuation tokens belong in `localStorage`, `sessionStorage`, URLs, or application state. The initial magic token uses a URL fragment and is removed immediately after the exchange.
