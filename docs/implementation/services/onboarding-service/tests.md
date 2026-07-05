# onboarding-service Tests

Run:

```powershell
cd onboarding-service
.\mvnw.cmd test
```

Current coverage:

```txt
OnboardingApplicationServiceTest
OnboardingApplicationWebAdapterTest
OnboardingServiceApplicationTests
```

The context test uses MySQL Testcontainers and validates the Flyway schema with Hibernate.

Use case tests isolate token generation, hashing, persistence, and notification delivery through ports.
