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

## Edge abuse control

The anonymous application-start command is the only current browser endpoint
that can create database/outbox work without prior identity or continuation
authority. The gateway therefore limits that exact method and path per client
before routing it to the BFF.

The limiter is deliberately local and bounded for the current single-gateway
stage. Per-client sliding windows are backed by an expiring cache; IPv6 client
keys aggregate to `/64`, and a process-wide window remains effective even if a
client bucket is evicted. It returns `429` with `Retry-After`; Angular explains
the wait without revealing whether an email exists. Source addresses come from
the socket by default. Forwarding headers become authoritative only for
explicitly trusted proxy CIDRs, and the HTTP server is forced not to reinterpret
them first.

This is an edge control, not business state. The per-email cooldown remains
durable in `onboarding-service`, while Keycloak owns failed-login lockouts. A
future multi-instance/VPS topology should replace the local cache with a shared
limiter or a correctly configured reverse proxy/WAF rather than creating a new
business microservice.
