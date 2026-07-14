# API Gateway Implementation

`api-gateway` is the external HTTP entry point. Its current route table intentionally contains one browser surface:

```txt
Path=/web/** -> lb://home-banking-bff
```

The route preserves the host header and resolves the BFF through Eureka. No prefix is stripped because the BFF runs with context path `/web`.

## Denied Surfaces

The gateway does not publish `/api/customers/**`, `/api/accounts/**`, or any direct internal service path. Its security filter permits `/web/**` and configured health/info endpoints, then denies the remainder.

Internal services keep their own OAuth2 resource-server policies. Calls between BFF/onboarding and those services use client credentials and service discovery, not the external gateway.

## Anonymous application rate limit

The gateway applies a local, bounded rate limit only to:

```txt
POST /web/onboarding/applications
```

Each client may start three requests per minute and ten per hour. Both are
sliding windows; rejected requests do not extend the wait. A rejection returns
`429 Too Many Requests`, a numeric `Retry-After`, `Cache-Control: no-store`, and
the stable `ONBOARDING_START_RATE_LIMIT` Problem Detail code. The response does
not contain the client address, email, counters, or downstream state.

The client key comes from the TCP peer. `Forwarded` and `X-Forwarded-For` are
ignored unless the immediate peer belongs to an explicitly configured trusted
proxy CIDR. When trust is enabled, the resolver accepts only literal IP
addresses and walks the forwarded chain from right to left. The boundary proxy
must replace untrusted incoming forwarding headers, not append to them.

State is held in a Caffeine cache capped at 10,000 clients, expires after two
hours of inactivity, and has a validated memory budget. IPv6 privacy addresses
share a configurable `/64` network key by default. A non-evictable process-wide
window also caps accepted starts at 30 per minute and 300 per hour, so rotating
addresses or evicting a client bucket cannot remove the local ceiling.

The limiter is intentionally local to one gateway process. A multi-instance
deployment must move this control to shared storage or an edge proxy/WAF. This
control complements the durable one-minute cooldown per email inside
`onboarding-service`; it does not replace it.

Polling, magic-link exchange, document submission, credential resend, login
callbacks, and authenticated endpoints are outside this limiter because they
have different authority and traffic patterns. Repeated password failures are
handled separately by Keycloak's temporary lockout policy.

## Configuration

```txt
config-repository/api-gateway.yaml
```

The default gateway port is `8085`. Config Server and Eureka locations are
environment-overridable. Rate-limit windows, capacities, cache bounds, and
trusted proxy CIDRs are externalized under
`banking.gateway.rate-limit.onboarding-start`.

## Verification

```powershell
cd api-gateway
.\mvnw.cmd test
```

Tests assert both sides of the boundary: `/web/**` is allowed, while former
direct business routes and unknown internal paths are denied. Deterministic
tests also cover both sliding windows, exact `Retry-After`, concurrency, cache
bounds, canonical IP handling, trusted proxy traversal, spoofed forwarding
headers, and the live WebFlux route.
