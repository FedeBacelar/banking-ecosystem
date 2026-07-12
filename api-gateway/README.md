# api-gateway

Edge gateway for the banking ecosystem. It owns no banking data or business rules.

## Public Boundary

The current browser boundary is intentionally narrow:

```txt
browser -> http://localhost:8085/web/** -> lb://home-banking-bff
```

`api-gateway` does not publish customer, account, identity, document, notification, or onboarding services directly. Service-to-service traffic uses Eureka and OAuth2 client credentials inside the trusted topology.

This prevents a browser from choosing internal resources or bypassing the BFF's session, CSRF, DTO, and composition policies.

## Security

The gateway permits `/web/**` and the configured health/info endpoints, then denies every other route. Browser authentication is owned by `home-banking-bff` through OIDC and server-side sessions.

The gateway is not the authorization owner for internal service APIs. Each internal service remains a resource server and validates the least-privilege machine token used by its caller.

## Configuration

Operational configuration comes from Config Server:

```txt
../config-repository/api-gateway.yaml
```

Local bootstrap configuration remains in `src/main/resources/application.yaml`.

## Local Runtime

Default URL:

```txt
http://localhost:8085
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Test:

```powershell
.\mvnw.cmd test
```

The security suite verifies that `/web/**` is admitted and direct `/api/**` or internal service paths are denied.
