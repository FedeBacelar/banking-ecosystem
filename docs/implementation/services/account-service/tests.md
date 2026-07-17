# account-service Tests

Current test command:

```powershell
cd account-service
.\mvnw.cmd test
```

Current verified result:

```txt
45 tests passing
```

## Test Types

### Domain Tests

Validate account rules without Spring infrastructure.

Current example:

```txt
AccountTest
```

### Use Case Tests

Validate account opening, lifecycle transitions, idempotency, and customer validation orchestration.

Current examples:

```txt
OpenAccountServiceTest
IdempotentAccountOpeningServiceTest
AccountLifecycleServiceTest
```

### Web Adapter Tests

Validate REST behavior, request validation, lifecycle endpoints, alias validation, and error responses.

Current example:

```txt
AccountWebAdapterTest
```

### Persistence Adapter Tests

Validate database mappings and repository behavior with MySQL Testcontainers.

Current examples:

```txt
AccountPersistenceAdapterIntegrationTest
IdempotentAccountOpeningIntegrationTest
```

### Security and Integration Tests

Validate endpoint capabilities, service-owned client credentials, authorization-header replacement,
and W3C trace propagation across the Account to Customer boundary.

Current examples:

```txt
SecurityConfigTest
OAuth2ClientCredentialsTokenAdapterTest
CustomerFeignTracePropagationIntegrationTest
```

### Observability Profile Tests

Validate that telemetry remains opt-in and that the Prometheus endpoint is available only when the
`observability` profile is enabled.

Current example:

```txt
ObservabilityProfileTest
```

## Testing Rule

When a database column has a length or constraint, the API request should validate it before MySQL rejects the insert.

This avoids errors like:

```txt
Data truncation: Data too long for column
```
