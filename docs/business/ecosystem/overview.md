# Banking Ecosystem Overview

The project models a banking ecosystem using business services with clear ownership.

The main idea is that each service owns one business capability and does not share its database with other services.

## Current Business Services

```txt
customer-service
account-service
identity-service
notification-service
document-service
onboarding-service
```

## Customer Service

`customer-service` owns the formal relationship between a person and the bank.

It answers questions such as:

```txt
- Who is this customer?
- What is the customer's operational status?
- Has the customer passed KYC?
- What document identifies this customer?
- What contact and address information was registered?
```

## Account Service

`account-service` owns bank accounts and their operational state.

It answers questions such as:

```txt
- Which accounts exist?
- Which customer owns each account?
- What type of account is it?
- What currency does the account use?
- What is the account status?
- What is the operational balance?
```

## Identity Service

`identity-service` owns the link between authenticated external identities and internal banking customers.

It answers questions such as:

```txt
- Which customer belongs to this authenticated identity?
- Is the identity link active?
- Which external identities are linked to a customer?
```

## Current Relationship Between Services

When an account is opened, `account-service` validates the customer through `customer-service`.

`account-service` stores only the external `customerId`. It does not copy personal customer data.

When a user logs in, future browser-facing components can resolve the authenticated identity through `identity-service`.

`identity-service` stores only the external `customerId`. It does not copy personal customer data.

## Notification Service

`notification-service` owns notification requests and delivery state.

It answers questions such as:

```txt
- Which notification was requested?
- Which template was used?
- Which recipient was targeted?
- Was the delivery sent or did it fail?
- What delivery error occurred?
```

The first implemented channel is email.

`notification-service` does not decide why a notification should be sent. Business services decide that a notification is needed, and `notification-service` handles templating, delivery, and delivery state.

## Document Service

`document-service` owns banking document metadata and the integration with object storage.

It answers questions such as:

```txt
- Which document was uploaded?
- Which business process owns it?
- What category is it?
- Where is the file stored?
- Was the file accepted by the storage boundary?
```

The first implemented use case is onboarding evidence, such as DNI front and back images.

`document-service` does not approve onboarding, review documents, own customer data, or expose public document downloads.

## Onboarding Service

`onboarding-service` owns the applicant workflow before a person becomes a bank customer.

It answers questions such as:

```txt
- Which onboarding application is active?
- Has the applicant verified email ownership?
- Which step is the application currently in?
- Is the application still valid or expired?
- Can the applicant continue the process?
```

The first implemented slice starts an application from an email address, sends a magic link through `notification-service`, and moves the application to `IN_PROGRESS` when the magic link is consumed.

`onboarding-service` does not own final customer records, bank accounts, document file storage, identity links, Keycloak authentication, or email delivery infrastructure.

## Business Principle

The ecosystem should grow by adding services with real banking responsibility, not by creating small CRUD wrappers around tables.
