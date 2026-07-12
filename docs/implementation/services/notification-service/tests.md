# notification-service Tests

Run from the service folder:

```powershell
.\mvnw.cmd test
```

Current coverage:

```txt
- Application context with Testcontainers MySQL.
- Email notification use case success and failure behavior.
- Template rendering and missing variable validation.
- Web adapter request validation and error mapping.
```

