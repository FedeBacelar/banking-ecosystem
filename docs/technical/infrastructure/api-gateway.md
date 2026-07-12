# API Gateway

The gateway is a thin edge router. It centralizes the public origin but does not duplicate authentication state, banking authorization, or service orchestration.

## Current Topology

```txt
Browser
  -> api-gateway:8085 /web/**
  -> home-banking-bff:8086 /web/**
```

All other services are private topology members discovered through Eureka:

```txt
home-banking-bff -> identity/customer/account/onboarding
onboarding-service -> document/notification/customer/account/identity/Keycloak
```

These internal calls use least-privilege client-credentials tokens. Routing them back through the gateway would blur trust boundaries and is intentionally avoided.

## Why The Gateway Does Not Expose Business APIs

Direct browser access would let clients bypass BFF-owned controls and force every frontend to understand internal service contracts. Keeping only `/web/**` provides:

- one public origin;
- server-owned browser sessions and CSRF;
- customer context derived from identity instead of browser input;
- stable public DTOs independent of internal service evolution;
- no accidental exposure of operational or provisioning endpoints.

Service-level resource servers remain mandatory because network location is not authorization.
