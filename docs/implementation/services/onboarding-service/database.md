# onboarding-service Database

Database:

```txt
onboarding_db
```

## Tables

```txt
onboarding_application
onboarding_applicant_data
onboarding_document_reference
onboarding_terms_acceptance
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

## onboarding_applicant_data

Stores the first structured applicant data step captured after email verification.

Important columns:

```txt
application_id
first_name
middle_name
last_name
birth_date
nationality
document_type
document_number
document_issuing_country
document_expiration_date
phone_number
street
street_number
city
province
postal_code
country
created_at
updated_at
version
```

The row is keyed by `application_id`, so saving applicant data is idempotent for the current onboarding application.

## onboarding_document_reference

Stores references to documents owned by `document-service`.

Important columns:

```txt
id
application_id
category
document_id
created_at
updated_at
version
```

Constraints and indexes:

```txt
uk_onboarding_document_reference_application_category
idx_onboarding_document_reference_application
idx_onboarding_document_reference_document
```

The unique key on `application_id` and `category` keeps one active reference per required document type, such as `DNI_FRONT` and `DNI_BACK`.

## onboarding_terms_acceptance

Stores the accepted terms version for the application.

Important columns:

```txt
application_id
terms_version
accepted_at
created_at
updated_at
version
```

The row is keyed by `application_id`, so accepting terms again updates the accepted version and timestamp.
