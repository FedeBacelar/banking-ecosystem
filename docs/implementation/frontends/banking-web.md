# banking-web

`banking-web` is an Angular standalone verification client. Its current onboarding UI is deliberately disposable; process rules and orchestration live behind the BFF.

## Current Flow

```txt
email -> check email -> consume fragment token -> composite submit
      -> backend status -> Keycloak credential actions -> completion
```

Public routes:

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue#token=...
/onboarding/applicant-data
/onboarding/status
/onboarding/credentials-complete
```

The magic token uses the URL fragment so it is not sent in the initial HTTP request or normal referrer data. The continuation token remains in an HttpOnly BFF cookie.

## Browser Responsibilities

- Collect and locally validate presentation-level input.
- Send one start request and one magic-link exchange request.
- Send one multipart submission with data, terms, and DNI front/back.
- Render the backend-provided `nextAction` and read status for the asynchronous process.
- Let Angular attach the XSRF header automatically.

The browser does not coordinate document-service, decide review/provisioning transitions, select customer IDs, persist tokens, fetch session metadata, or bootstrap CSRF explicitly.

## Local Verification

```powershell
cd banking-web
npm.cmd start
npm.cmd test -- --watch=false
npm.cmd run build
```

The proxy routes `/web` to `http://localhost:8085`. The final onboarding and home-banking frontend design is a separate feature built on these contracts.
