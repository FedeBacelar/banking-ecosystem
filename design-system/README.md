# Nerva design system

This directory contains the small, framework-independent source of truth for the Nerva visual identity used by customer-facing applications and Keycloak.

Canonical sources:

- `tokens.json`: colors, typography, radii, and shadows.
- `assets`: Nerva logos and the self-hosted Geist variable font.
- `generated/nerva-tokens.css`: generated CSS custom properties.

The generator also publishes framework-ready copies to Keycloak and Angular. Angular imports `banking-web/src/styles/_nerva-tokens.css` and serves the synchronized brand/font assets from `banking-web/public/assets`.

Generate the CSS and synchronize the Keycloak copies from the repository root:

```powershell
node design-system/scripts/generate.mjs
```

Verify that committed generated files match their sources without modifying them:

```powershell
node design-system/scripts/generate.mjs --check
```

Generated files must not be edited directly. Product-specific components and layouts do not belong in this directory.

Geist is supplied by `@fontsource-variable/geist` 5.2.9 and distributed under the SIL Open Font License 1.1. Its license is kept next to the canonical font asset.
