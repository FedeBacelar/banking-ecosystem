# banking-web

Angular verification client for Nerva Banking. The current UI proves the onboarding contracts; it is not the final guided banking frontend and may be replaced.

## Demo Flow

```txt
/onboarding/start
/onboarding/check-email
/onboarding/continue#token=...
/onboarding/applicant-data
/onboarding/status
/onboarding/credentials-complete
```

Browser calls are limited to relative `/web/**` URLs, which the development proxy sends to `api-gateway`.

The demo performs:

1. one request to start with an email;
2. one request to exchange the magic link;
3. one multipart request to submit data, terms, and both DNI files;
4. status reads while the asynchronous backend process advances.

It never calls an internal service, stores an access token, requests session metadata, or explicitly fetches CSRF. Angular's HTTP integration sends the XSRF header automatically after the BFF creates the cookie.

## Development

```powershell
npm.cmd start
```

Open `http://localhost:4200/onboarding/start`. The proxy maps `/web` to `http://localhost:8085`.

Real email requires the notification SMTP environment and the onboarding infrastructure to be running.

## Verification

```powershell
npm.cmd test -- --watch=false
npm.cmd run build
```
