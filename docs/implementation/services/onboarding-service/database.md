# onboarding-service Database

Database:

```txt
onboarding_db
```

## Tables

```txt
onboarding_application
```

## onboarding_application

Stores the current onboarding application and token lifecycle metadata.

Important columns:

```txt
id
email
status
magic_link_token_hash
magic_link_expires_at
magic_link_consumed_at
email_verified_at
continuation_token_hash
continuation_expires_at
expires_at
created_at
updated_at
version
```

Indexes:

```txt
uk_onboarding_magic_link_token_hash
uk_onboarding_continuation_token_hash
idx_onboarding_email_status
idx_onboarding_status
idx_onboarding_expires_at
```

Raw magic link and continuation tokens are never stored.
