# banking-web

Customer-facing Angular application for Nerva Banking. It is the single SPA for
public journeys and the authenticated home-banking branch.

## Current scope

The application currently implements both the access journey and the complete
application-capture journey:

```txt
/ -> /web/auth/login/home -> Keycloak -> /app/inicio -> /web/logout
/ -> /onboarding -> Mailpit -> magic link -> five-step application -> confirmation
```

Public routes:

```txt
/
/error
/sesion-expirada
/sesion-cerrada
/onboarding
/onboarding/correo-enviado
/onboarding/continue
/onboarding/continuar
/onboarding/solicitud
/onboarding/solicitud-enviada
/legales/terminos
/legales/privacidad
```

`/app/inicio` is lazy-loaded, requires `GET /web/me`, and intentionally shows
only the "En construcción" state.

The onboarding feature starts with an enumeration-safe email request, consumes
the magic-link secret from the URL fragment, and removes that fragment before
calling the BFF. Its five-step wizard uses Angular Signal Forms and translates
human-facing Argentina values to the current multipart API contract. Draft PII,
the magic-link token, and both document files remain in memory only. No draft is
written to URLs, browser storage, or application logs.

The landing page exposes only implemented actions: open an account or sign in.
Angular, email, and Keycloak show a visible academic disclaimer and explicitly
ask visitors not to enter real personal, banking, password, or document data.

## Architecture

- Angular 22 standalone, strict, and zoneless.
- Feature-first routes under `features/public`, `features/onboarding`,
  `features/legal`, and `features/home-banking`.
- Shared shells and brand primitives under `shared`.
- Session and browser-security concerns under `core`.
- Tailwind CSS 4, selective Angular CDK, Lucide icons, and Angular Signal Forms.

Browser requests use relative `/web/**` URLs. The development proxy sends them
to `api-gateway`; the SPA never stores OAuth tokens or calls internal services.
Login and logout are top-level browser navigations. Logout is a normal HTML
`POST` with the CSRF value materialized by `/web/me`.

If the gateway limits repeated application starts, Angular reads the numeric
`Retry-After` header and tells the customer how long to wait. The screen does
not display the client address, request counters, or any email-registration
state.

## Visual source of truth

The canonical Nerva tokens, logos, and Geist font live in `../design-system`.
Do not edit these generated copies directly:

```txt
src/styles/_nerva-tokens.css
public/assets/brand/*
public/assets/fonts/*
```

Regenerate or verify them from the repository root:

```powershell
node design-system/scripts/generate.mjs
node design-system/scripts/generate.mjs --check
```

## Development

```powershell
npm.cmd install
npm.cmd start
```

Open `http://localhost:4200/`. The BFF, gateway, Keycloak, and subject-linked
local customer seed must be available for the authenticated route. For the
application journey, start Mailpit and the notification, onboarding, and
document services as well:

```powershell
docker compose -f ..\infra\mailpit\docker-compose.yml up -d
```

Mailpit is available at `http://localhost:8025`.

## Verification

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd audit --omit=dev
```
