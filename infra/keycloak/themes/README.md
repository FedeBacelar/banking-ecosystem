# Keycloak themes

This folder contains local Keycloak themes mounted by Docker Compose.

Current theme:

```txt
banking
```

Current scope:

```txt
banking/login
```

The theme is responsible only for authentication screens rendered by Keycloak. Banking product screens belong to `banking-web` and are not implemented inside Keycloak.

Visual tokens, logos, and the Geist font come from the repository-level `design-system`. Do not edit generated copies inside this directory. Synchronize or verify them with:

```powershell
node design-system/scripts/generate.mjs
node design-system/scripts/generate.mjs --check
```

Current covered screens:

```txt
login
error
info
login-page-expired
logout-confirm
```

Banking access requests, password recovery and long-lived browser sessions are not enabled from the public login screen. Those flows should be designed as controlled banking operations before exposing them to users.

The `keycloak-realm-init` container applies the theme and Spanish-only locale to both new and existing local realms. To verify it manually:

```txt
Realm settings -> Themes -> Login theme -> banking
```

The return URL uses `BANKING_FRONTEND_URL` and defaults to `http://localhost:4200`; it never probes a BFF session endpoint.
