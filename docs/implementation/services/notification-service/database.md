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
subject
body
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
```

