# banking-web

`banking-web` is the real customer-facing Angular SPA for Nerva Banking. Public
onboarding and authenticated home banking share one deployable application,
while their routes, state, and components remain separated by feature.

## Runtime boundary

```txt
browser -> Angular -> api-gateway /web/** -> home-banking-bff
                         |
                         +-> Keycloak for authentication screens
```

Angular never receives OAuth access or refresh tokens. The BFF owns the OIDC
session, resolves the Keycloak subject to the internal customer, and exposes
browser-specific contracts.

## Current route map

```txt
public
  /
  /error
  /sesion-expirada
  /sesion-cerrada

authenticated (lazy)
  /app/inicio
```

The authenticated route calls `GET /web/me`. A missing session becomes the
customer-facing expired-session state; other failures use the generic access
error state. `/app/inicio` is intentionally limited to "En construcción" until
the home-banking product scope begins.

The landing page avoids unsupported product promises and has a single sign-in
action. Every public access surface, including Keycloak, identifies Nerva as an
academic project rather than a financial institution and asks visitors not to
use real data. Session and error routes use a dedicated minimal shell so a
post-logout screen never repeats header, card, and footer sign-in actions.

## Access journey

```txt
Portada
  -> GET /web/auth/login/home
  -> Keycloak
  -> OIDC callback owned by the BFF
  -> /app/inicio
  -> POST /web/logout with CSRF
  -> Keycloak logout
  -> /sesion-cerrada
```

`GET /web/me` returns only `username` and `displayName` and materializes the
readable CSRF cookie required by the top-level logout form. Navigating back
after logout cannot restore the authenticated area because the guard validates
the server session again.

## Frontend structure

```txt
src/app
  core/                 session and browser-security concerns
  features/public/      landing and access states
  features/home-banking authenticated shell and placeholder
  shared/               brand and layout primitives
```

The application uses Angular 22 standalone components, strict mode, zoneless
change detection, lazy feature routes, Tailwind CSS 4, selective Angular CDK,
Lucide icons, and Signal Forms for later customer forms.

## Design system integration

`design-system/tokens.json`, the Nerva SVG assets, and the self-hosted Geist
font are canonical. `design-system/scripts/generate.mjs` synchronizes generated
copies into Angular and the Keycloak theme; generated files must not be edited
by hand.

## Verification

```powershell
cd banking-web
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd audit --omit=dev
cd ..
node design-system/scripts/generate.mjs --check
```
