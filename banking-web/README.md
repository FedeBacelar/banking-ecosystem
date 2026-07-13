# banking-web

Customer-facing Angular application for Nerva Banking. It is the single SPA for
public journeys and the authenticated home-banking branch.

## Current scope

Step 1 implements the complete access journey:

```txt
/ -> /web/auth/login/home -> Keycloak -> /app/inicio -> /web/logout
```

Public routes:

```txt
/
/error
/sesion-expirada
/sesion-cerrada
```

`/app/inicio` is lazy-loaded, requires `GET /web/me`, and intentionally shows
only the "En construcción" state. The guided onboarding journey will be added
in a later construction step.

The landing page exposes one real action: sign in. Angular and Keycloak show a
visible academic disclaimer and explicitly ask visitors not to enter real
personal, banking, or password data. Access error, inactive-session, and
post-logout routes use a minimal shell without duplicated sign-in actions.

## Architecture

- Angular 22 standalone, strict, and zoneless.
- Feature-first routes under `features/public` and `features/home-banking`.
- Shared shells and brand primitives under `shared`.
- Session and browser-security concerns under `core`.
- Tailwind CSS 4, selective Angular CDK, Lucide icons, and Angular Signal Forms.

Browser requests use relative `/web/**` URLs. The development proxy sends them
to `api-gateway`; the SPA never stores OAuth tokens or calls internal services.
Login and logout are top-level browser navigations. Logout is a normal HTML
`POST` with the CSRF value materialized by `/web/me`.

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
local customer seed must be available for the authenticated route.

## Verification

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
npm.cmd audit --omit=dev
```
