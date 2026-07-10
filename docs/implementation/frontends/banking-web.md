# banking-web

`banking-web` is an Angular 22 standalone application. Its current onboarding UI is a functional verification client, not the final guided banking experience.

## Implemented Demo Flow

```txt
email -> check email -> consume magic link -> applicant data
      -> DNI front/back -> terms -> submit -> status
      -> credential invitation -> Keycloak actions -> completion page
```

The applicant page chains the BFF operations in order and submits only after every required call succeeds. The status page polls the safe BFF status contract and supports invitation resend when credentials are pending.

## Architecture

```txt
src/app/core/api
src/app/shared/ui
src/app/features/onboarding
```

Routes are lazy loaded. Browser code calls only relative `/web/**` URLs. The BFF owns the continuation token as an HttpOnly cookie; Angular obtains a CSRF cookie before each mutation and does not store access tokens.

## Routes

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue?token=...
/onboarding/session
/onboarding/applicant-data
/onboarding/status
/onboarding/credentials-complete
```

## Local Runtime

```powershell
cd banking-web
npm start
```

Open `http://localhost:4200/onboarding/start`. The dev proxy sends `/web` to `http://localhost:8085` (api-gateway).

## Verification

```powershell
npm test -- --watch=false
npm run build
```

The final guided form, banking-grade visual system, and authenticated home-banking UI remain a separate frontend feature. The current BFF contracts are intended to support that replacement without changing the process owner.
