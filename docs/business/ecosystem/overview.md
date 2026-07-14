# Banking Ecosystem Overview

The project models a banking ecosystem through services with explicit business ownership. Each stateful service owns its database and exchanges data only through documented contracts.

## Current Business Capabilities

```txt
customer-service
account-service
identity-service
notification-service
document-service
onboarding-service
```

## Customer

`customer-service` owns the formal relationship between a person and the bank:

- natural-person registration;
- customer lifecycle status;
- KYC state and KYC history;
- identifying document data;
- registered contact and address data.

It does not own account, credential, onboarding application, or transaction state.

## Account

`account-service` owns bank accounts:

- account identifiers and aliases;
- account type and currency;
- account lifecycle and status history;
- current, available, and held balance positions.

It stores `customerId` as an external reference and validates customer eligibility through `customer-service`. It does not copy customer PII or KYC records.

## Identity

`identity-service` owns the link between an external authenticated subject and a banking customer:

```txt
provider + providerSubject -> customerId
```

The link is unique by provider subject and by customer/provider. The service does not authenticate users and does not own customer master data.

## Notification

`notification-service` owns notification requests, templates, delivery attempts, redacted audit records for sensitive messages, and provider delivery state.

Business services decide when a notification is required. `notification-service` renders and delivers it. Email is the implemented channel.

## Document

`document-service` owns evidence metadata and object-storage integration. It validates upload size, accepted content type, file signature, hash, business context, category, and idempotency key.

The implemented onboarding categories are `DNI_FRONT` and `DNI_BACK`. The service stores evidence but does not approve an onboarding application or expose public document downloads.

## Onboarding

`onboarding-service` owns the application process for a person who does not yet have a banking customer, account, identity link, or usable credentials.

It controls:

- email ownership verification and recoverable continuation access;
- applicant data, evidence references, and terms acceptance;
- submit integrity and state transitions;
- automated review evidence and decisions;
- durable provisioning across capability owners;
- credential invitation and completion reconciliation.

After approval, it coordinates customer, account, identity, document, notification, and Keycloak capabilities without taking ownership of their resources.

## Current Interaction Model

```txt
browser -> api-gateway /web/** -> home-banking-bff

home-banking-bff -> onboarding-service
home-banking-bff -> identity-service -> customer-service / account-service

onboarding-service -> notification-service
onboarding-service -> document-service
onboarding-service -> customer-service
onboarding-service -> account-service
onboarding-service -> identity-service
onboarding-service -> Keycloak

account-service -> customer-service
```

Internal calls use purpose-specific service credentials and service discovery where applicable. `account-service` uses its own read-only machine token when validating a customer instead of forwarding its caller's token. Services never query another service database.

## Boundary Principle

The ecosystem grows by adding capabilities with real banking responsibility, not by wrapping every table in a separate service. Orchestration does not transfer ownership of the resources being coordinated.
