# config-server

Centralized configuration server for the banking ecosystem.

This service exposes configuration stored in the repository-level `config-repository` directory using Spring Cloud Config Server in native mode.

## Responsibility

- Serve externalized configuration to ecosystem services.
- Keep service runtime configuration outside each service package.
- Provide a local learning-friendly setup that can later evolve to a dedicated Git-backed configuration repository.

## Local Port

```txt
http://localhost:8888
```

## Configuration Source

By default, the server reads configuration from:

```txt
../config-repository
```

The location can be overridden with:

```txt
CONFIG_REPOSITORY_LOCATION
```

## Useful URLs

```txt
http://localhost:8888/application/default
http://localhost:8888/eureka-server/default
http://localhost:8888/customer-service/default
http://localhost:8888/account-service/default
http://localhost:8888/api-gateway/default
```

## Run Locally

From this directory:

```powershell
.\mvnw.cmd spring-boot:run
```

## Notes

The configuration repository must not contain real secrets. Sensitive values should be provided through environment variables, local `.env` files, CI/CD secrets, or a secret manager.
