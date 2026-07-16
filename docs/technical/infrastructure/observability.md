# Onboarding observability

Nerva uses a portable observability contract for the customer onboarding
journey. The applications emit standard telemetry; they do not call Grafana,
Prometheus, Loki, or Tempo directly.

```txt
API Gateway -> Home Banking BFF -> Onboarding -> Notification
      |                 |               |              |
      +------- metrics, logs, and traces via OpenTelemetry -------+
                                      |
                               local LGTM stack
                         Prometheus + Loki + Tempo
                                      |
                                   Grafana
```

## Signals

- Prometheus scrapes technical and bounded business metrics from
  `/actuator/prometheus` every 15 seconds.
- OpenTelemetry exports W3C distributed traces and structured log records over
  OTLP HTTP.
- The existing `X-Correlation-Id` remains a functional support reference. It is
  deliberately separate from the internal `traceId`.
- Durable workers create a new root trace for each execution instead of keeping
  an HTTP trace open while work waits in the database.

The `Nerva · Onboarding` dashboard combines two views: service health and the
customer journey. Its configuration is versioned under `infra/observability`.

## Runtime contract

Observability is opt-in through the `observability` Spring profile. Without the
profile, the OpenTelemetry SDK is disabled. Export is asynchronous, bounded,
and fail-open, so an unavailable Collector must never reject customer traffic
or stop a worker.

The portable runtime settings are:

```txt
OTEL_SDK_DISABLED
OTEL_EXPORTER_OTLP_ENDPOINT
OTEL_SERVICE_NAME
OTEL_RESOURCE_ATTRIBUTES
```

Local development points OTLP at `http://localhost:4318`. A later VPS deployment
can point the same applications at an OpenTelemetry Collector without changing
application code.

## Privacy and cardinality

Telemetry must not contain email addresses, DNI values, usernames, IP
addresses, credentials, tokens, links, documents, authorization headers,
request bodies, query strings, or exception messages that may expose those
values.

Metric and log dimensions use finite values such as job type, template, event,
and outcome. Application, customer, notification, and correlation identifiers
are never metric or Loki labels. Database query sanitization remains enabled.

Before persistence, the Collector removes known semantic attributes for URL
queries and full URLs, network addresses, authentication, identity, process
command lines, exception messages, and stack traces. Exception type, trace
context, and safe operational attributes remain available for diagnosis. This
is defense in depth: application code must never place personal data, tokens,
or sensitive links in a normal log body.

## Development platform

`grafana/otel-lgtm` packages the development backends into one container. It is
appropriate for a portfolio laboratory, demos, and local diagnosis, but it is
not the future production topology. Prometheus retains seven days or 1 GiB;
Loki and Tempo retain three days. Only Grafana and OTLP HTTP bind to host
loopback.

The image bundles Pyroscope, but Nerva defines no profiling pipeline,
datasource, or host port. Profiling is therefore outside this stage. Prometheus
endpoints are development/private-network interfaces; the Gateway blocks
`/web/actuator/**`, and service ports must never be exposed directly to the
public Internet.

In the future VPS topology, Collector, Prometheus, Loki, Tempo, and Grafana will
be separate containers on a private network. The instrumentation, signal names,
privacy rules, dashboards, and tests introduced here remain reusable.
