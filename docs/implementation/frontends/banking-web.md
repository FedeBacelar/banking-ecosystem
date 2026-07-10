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
- Applicant data form for the first wizard step.
- DNI front/back upload controls.
- Terms acceptance capture.
- Local development proxy for /web.
```

Not implemented yet:

```txt
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

The applicant-data screen currently saves applicant data, uploads `DNI_FRONT` and `DNI_BACK`, and accepts the current onboarding terms version through BFF endpoints. The UI is functional first-slice quality; guided banking controls and final styling are planned for a later iteration.

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
http://localhost:8085
```

The development proxy points to `api-gateway`. Browser code must keep using `/web/**`; it must not call `home-banking-bff` or internal services directly.

## Routes

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue?token=...
/onboarding/session
/onboarding/applicant-data
```

## Tests and Build

```powershell
npm test -- --watch=false
npm run build
```
