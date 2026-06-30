# Config Server

`config-server` centralizes externalized configuration for the local banking ecosystem.

The service is implemented with Spring Cloud Config Server and currently runs in native mode, reading configuration from the repository-level `config-repository` directory.

## Responsibilities

- Serve configuration by application name and profile.
- Keep operational configuration outside individual service packages.
- Provide a local setup that can later move to a dedicated Git-backed configuration repository.

## Current Configuration Source

```txt
config-repository/
```

The configuration source is kept inside this monorepo so the project remains self-contained for local development and review.

## Runtime Flow

```txt
service startup
  -> config-server
    -> config-repository
```

For example, `customer-service` reads its application name locally and imports configuration from `config-server`.

## Secrets

Real secrets must not be committed to the repository.

Configuration files can reference environment variables and may include local development defaults, but production credentials, API keys, tokens, private keys, and signing secrets belong in external secret storage.
