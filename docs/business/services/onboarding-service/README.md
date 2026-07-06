# onboarding-service

`onboarding-service` owns the digital onboarding journey for an applicant who starts from zero.

The applicant does not yet have:

```txt
- Banking user.
- Customer id.
- Account.
- Identity link.
```

## Business Capability

The service provides a controlled workflow to receive an application, verify email ownership, continue the applicant journey, and later coordinate review and provisioning.

The first implemented slice covers:

```txt
- Start onboarding from an email address.
- Send a one-time magic link.
- Verify the magic link.
- Create a continuation token for the BFF.
- Keep application state and expiration.
```

## Ownership

The service owns:

```txt
- Onboarding application state.
- Applicant email verification state.
- Magic link token lifecycle.
- Continuation token lifecycle.
- Onboarding state transitions.
- Application expiration.
```

The service does not own:

```txt
- Final customer master data.
- Bank accounts.
- Uploaded file content.
- Email delivery mechanics.
- Keycloak users.
- Identity links.
```

## Current Business Rule

An applicant can have only one active onboarding application per email.

If the active application is still waiting for email verification, asking for a new link reuses the same application, rotates the magic-link token, and sends a new email. This keeps the operation user-friendly without creating duplicate onboarding records.

Magic links are one-time tokens. Raw tokens are never persisted; only hashes are stored.

The current service is an internal foundation. The browser-facing public onboarding route will be exposed later through `home-banking-bff` under `/web`.
