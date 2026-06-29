# account-service Tests

Current test command:

```powershell
cd account-service
.\mvnw.cmd test
```

Current verified result:

```txt
17 tests passing
```

## Test Types

### Domain Tests

Validate account rules without Spring infrastructure.

Current example:

```txt
AccountTest
```

### Use Case Tests

Validate account opening behavior and customer validation orchestration.

Current example:

```txt
OpenAccountServiceTest
```

### Web Adapter Tests

Validate REST behavior, request validation, lifecycle endpoints, alias validation, and error responses.

Current example:

```txt
AccountWebAdapterTest
```

### Persistence Adapter Tests

Validate database mappings and repository behavior with MySQL Testcontainers.

Current example:

```txt
AccountPersistenceAdapterIntegrationTest
```

## Testing Rule

When a database column has a length or constraint, the API request should validate it before MySQL rejects the insert.

This avoids errors like:

```txt
Data truncation: Data too long for column
```
