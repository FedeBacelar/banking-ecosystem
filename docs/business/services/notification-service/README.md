# Notification Service Business View

`notification-service` represents banking communications requested by the ecosystem.

It is not an onboarding decision service. It records and sends notifications that another service has decided are necessary.

## Owns

```txt
- Notification requests.
- Email recipients.
- Template codes and template variables.
- Delivery status.
- Delivery attempts.
- Provider delivery errors.
```

## Does Not Own

```txt
- Customer onboarding decisions.
- Customer data.
- Account data.
- Identity links.
- Authentication users.
```

## Current Scope

The current implementation supports templated email notifications through SMTP.

Current templates:

```txt
ONBOARDING_EMAIL_MAGIC_LINK
ONBOARDING_APPROVED_CREDENTIAL_INVITATION
ONBOARDING_REJECTED
ONBOARDING_COMPLETED
```

