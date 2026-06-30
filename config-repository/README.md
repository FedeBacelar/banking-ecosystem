# Configuration Repository

This directory contains externalized configuration served by `config-server`.

The current setup uses Spring Cloud Config Server in native mode, which reads these files directly from the local filesystem.

## Files

```txt
application.yaml
customer-service.yaml
account-service.yaml
eureka-server.yaml
```

`application.yaml` contains shared configuration. Service-specific files contain configuration for each application name.

## Sensitive Values

Do not store real secrets in this directory.

Configuration files may reference environment variables such as:

```txt
CUSTOMER_DB_PASSWORD
ACCOUNT_DB_PASSWORD
EUREKA_SERVER_URL
```

Local development may use safe defaults, but real credentials must come from outside the repository.
