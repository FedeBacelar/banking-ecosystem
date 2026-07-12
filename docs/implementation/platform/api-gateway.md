# API Gateway Implementation

`api-gateway` is the external HTTP entry point. Its current route table intentionally contains one browser surface:

```txt
Path=/web/** -> lb://home-banking-bff
```

The route preserves the host header and resolves the BFF through Eureka. No prefix is stripped because the BFF runs with context path `/web`.

## Denied Surfaces

The gateway does not publish `/api/customers/**`, `/api/accounts/**`, or any direct internal service path. Its security filter permits `/web/**` and configured health/info endpoints, then denies the remainder.

Internal services keep their own OAuth2 resource-server policies. Calls between BFF/onboarding and those services use client credentials and service discovery, not the external gateway.

## Configuration

```txt
config-repository/api-gateway.yaml
```

The default gateway port is `8085`. Config Server and Eureka locations are environment-overridable.

## Verification

```powershell
cd api-gateway
.\mvnw.cmd test
```

Tests assert both sides of the boundary: `/web/**` is allowed, while former direct business routes and unknown internal paths are denied.
