# customer-service Tests

Current test command:

```powershell
cd customer-service
.\mvnw.cmd test
```

Current verified result:

```txt
43 tests passing
```

## Test Types

### Domain Tests

Validate business rules without Spring infrastructure.

Current example:

```txt
NaturalPersonCustomerTest
```

### Use Case Tests

Validate application behavior and domain orchestration.

Current examples:

```txt
RegisterNaturalPersonCustomerServiceTest
CustomerLifecycleUseCaseTest
```

### Web Adapter Tests

Validate REST behavior, request validation, and error responses.

Current example:

```txt
CustomerWebAdapterTest
```

### Persistence Adapter Tests

Validate database mappings and repository behavior with MySQL Testcontainers.

Current example:

```txt
CustomerPersistenceAdapterIntegrationTest
```

### E2E Tests

Validate the service through the API and persistence stack.

Current example:

```txt
CustomerApiE2ETest
```

### Security and Observability Tests

Validate endpoint capabilities and confirm that telemetry remains opt-in, with Prometheus exposed
only under the `observability` profile.

Current examples:

```txt
SecurityConfigTest
ObservabilityProfileTest
```

## Testing Rule

When a database column has a length or constraint, the API request should validate it before MySQL rejects the insert.

This avoids errors like:

```txt
Data truncation: Data too long for column
```
