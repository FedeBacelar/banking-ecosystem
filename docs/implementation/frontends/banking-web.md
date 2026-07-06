# banking-web

`banking-web` is the Angular frontend for the banking ecosystem.

The first implemented slice covers public digital onboarding.

## Current Scope

Implemented:

```txt
- Angular 22 standalone application.
- Lazy loaded onboarding routes.
- Public email capture screen.
- Check-email confirmation screen.
- Magic-link continuation screen.
- Onboarding session state screen.
- Local development proxy for /web.
```

Not implemented yet:

```txt
- Applicant data wizard.
- Document upload UI.
- Submit application UI.
- Credential setup UI.
- Authenticated home banking screens.
```

## Architecture

Current structure:

```txt
src/app/core/api
src/app/shared/ui
src/app/features/onboarding
```

The frontend calls only the BFF:

```txt
banking-web -> /web/onboarding/** -> api-gateway -> home-banking-bff
```

It does not call `onboarding-service` directly and does not store access tokens in browser storage.

The onboarding continuation token is handled by the BFF as an HttpOnly cookie.

## Local Runtime

Run:

```powershell
cd banking-web
npm start
```

Open:

```txt
http://localhost:4200/onboarding/start
```

The Angular dev server proxies `/web` to:

```txt
http://localhost:8086
```

The development proxy points directly to `home-banking-bff` to avoid local Vite proxy parsing issues with gateway responses. Browser code still uses `/web/**`, and the intended runtime entrypoint remains `api-gateway`.

## Routes

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue?token=...
/onboarding/session
```

## Tests and Build

```powershell
npm test -- --watch=false
npm run build
```
