# Service Boundaries

Boundaries follow business capabilities, not tables or transport concerns. A service owns its domain state and database; callers use APIs and never read another service database.

## customer-service

Owns formal customer registration, lifecycle, KYC state, identification, contact data, and addresses.

Does not own accounts, credentials, onboarding applications, identity-provider subjects, or transactions.

## account-service

Owns account identifiers, aliases, product type, currency, lifecycle, balance position, and account status history.

It stores only an external `customerId`. It does not own customer PII, KYC, identity links, transaction history, or exchange rates.

## identity-service

Owns the durable link between `provider + providerSubject` and `customerId`, including link status and uniqueness rules.

It does not authenticate users, issue tokens, or own banking customer data.

## notification-service

Owns notification templates, delivery requests, channel status, delivery attempts, and sanitized provider failures.

It does not decide why a banking notification is sent and does not own onboarding or customer state.

## document-service

Owns document metadata, accepted upload constraints, object keys, storage status, content hashes, and object-storage integration.

It does not approve evidence, decide onboarding, own customer data, or expose public document downloads.

## onboarding-service

Owns onboarding applications, applicant continuation access, applicant capture, terms, review evidence, workflow state, status history, work items, provisioning progress, and process recovery.

It coordinates other capability owners but does not own the final customer, account, file content, Keycloak credential, notification record, or identity link.

## home-banking-bff

Owns the browser-facing contract for the web channel: OAuth2 login integration, server-side browser session, onboarding cookie, CSRF boundary, public DTOs, safe errors, and response composition.

It does not own banking domain state and is not a public replacement for internal service APIs.

## Platform Boundaries

- `api-gateway` is the external HTTP route boundary and publishes `/web/**` to the BFF.
- Keycloak authenticates people and machine clients and issues OAuth2/OIDC tokens.
- Config Server distributes runtime configuration.
- Eureka provides local service discovery.
- MinIO provides object storage behind `document-service`.

Platform components do not own banking business decisions.

## Current Communication Graph

```txt
browser -> api-gateway -> home-banking-bff

home-banking-bff -> onboarding-service
home-banking-bff -> identity-service
home-banking-bff -> customer-service
home-banking-bff -> account-service

onboarding-service -> notification-service
onboarding-service -> document-service
onboarding-service -> customer-service
onboarding-service -> account-service
onboarding-service -> identity-service
onboarding-service -> Keycloak Admin API

account-service -> customer-service
```

Browser credentials are not propagated through this graph. The BFF and onboarding orchestrator obtain purpose-specific client-credentials tokens. `account-service` currently forwards its inbound internal bearer token when validating a customer through `customer-service`.
