# Banking Ecosystem Overview

The project models a banking ecosystem using business services with clear ownership.

The main idea is that each service owns one business capability and does not share its database with other services.

## Current Business Services

```txt
customer-service
account-service
identity-service
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

## Business Principle

The ecosystem should grow by adding services with real banking responsibility, not by creating small CRUD wrappers around tables.
