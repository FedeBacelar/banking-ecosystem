# notification-service Database

Database:

```txt
notification_db
```

## Tables

```txt
notification
```

## notification

Stores requested notifications, rendered content, delivery status, and delivery error information.

Important columns:

```txt
id
channel
recipient
template_code
variables_json
correlation_id
request_fingerprint
subject
body
html_body
status
attempt_count
last_error
sent_at
created_at
updated_at
version
```

Indexes:

```txt
idx_notification_recipient
idx_notification_template_code
idx_notification_status
idx_notification_correlation_id
idx_notification_created_at
uk_notification_template_correlation
```

`(template_code, correlation_id)` is unique so an idempotent retry reuses the same
notification. `request_fingerprint` binds that retry to the original request without
retaining its sensitive variables. For onboarding templates, persisted variables and
rendered text/HTML bodies are redacted after rendering.

Flyway migrations `V1` through `V4` create the current schema.

