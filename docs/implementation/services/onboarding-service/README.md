# onboarding-service

`onboarding-service` stores onboarding applications and controls the first email verification step of the digital onboarding flow.

## Current Scope

Implemented:

```txt
- Start an onboarding application from email.
- Re-send a magic link for an existing EMAIL_VERIFICATION_PENDING application.
- Generate one-time magic link tokens.
- Hash magic link and continuation tokens before persistence.
- Send the magic link through notification-service.
- Consume a magic link once.
- Move an application from EMAIL_VERIFICATION_PENDING to IN_PROGRESS.
- Generate a continuation token for the BFF cookie flow.
- Validate continuation tokens.
```

Not implemented yet:

```txt
- Public BFF onboarding endpoints.
- Applicant data capture.
- Document requirements.
- Automated review checks.
- Customer/account provisioning.
- Credential setup invitation.
```

## Local Runtime

Configuration is served by `config-server` from:

```txt
config-repository/onboarding-service.yaml
```

Default port:

```txt
8087
```

Run:

```powershell
cd onboarding-service
.\mvnw.cmd spring-boot:run
```

Swagger:

```txt
http://localhost:8087/swagger-ui.html
```

## Security

The service validates JWT access tokens issued by Keycloak.

Roles:

```txt
ONBOARDING_READ
ONBOARDING_WRITE
```

Current local manual tests use the shared `banking-admin` operational user. The project intentionally avoids one Keycloak user per service.

## Integration

`onboarding-service` calls `notification-service` to request magic-link email delivery.

The first slice forwards the current bearer token when calling downstream services. When the public BFF onboarding route is added, the BFF will own the browser-facing session/cookie boundary.

## Idempotency

Starting onboarding with an email that already has an active EMAIL_VERIFICATION_PENDING application does not create a duplicate application. The service rotates the magic-link token, extends the magic-link expiration, and sends a fresh email for the existing application.

Starting onboarding with an email whose active application has already moved past email verification remains a conflict. The applicant must continue the existing journey instead of creating a second active application.

See also:

```txt
docs/implementation/services/onboarding-service/api.md
docs/implementation/services/onboarding-service/database.md
docs/implementation/services/onboarding-service/tests.md
docs/database/schema.dbml
```
