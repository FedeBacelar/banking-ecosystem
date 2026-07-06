# banking-web

Angular frontend for Nerva Banking.

The first implemented slice supports the public digital onboarding entry flow.

## Current Scope

Implemented:

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue?token=...
/onboarding/session
```

The app calls only the BFF through `/web/onboarding/**`.

It does not call internal business services directly and does not store access tokens in browser storage.

## Development server

From this folder:

```powershell
npm start
```

Open:

```txt
http://localhost:4200/onboarding/start
```

The local dev server proxies:

```txt
/web -> http://localhost:8085
```

## Required Backend

For the full email flow, run:

```txt
config-server
eureka-server
api-gateway
keycloak
notification-service
onboarding-service
home-banking-bff
```

`notification-service` must have SMTP environment variables configured to send real email.

## Build

```powershell
npm run build
```

## Tests

```powershell
npm test -- --watch=false
```
