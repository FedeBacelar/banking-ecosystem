# Validation

Validation should happen before data reaches the database.

The goal is to return clear `400 Invalid request` responses instead of SQL truncation or constraint errors when input is invalid.

## Current Approach

Request DTOs use Jakarta Validation annotations:

```txt
@NotNull
@NotBlank
@Past
@Pattern
@Size
@Valid
```

## Database Length Alignment

String fields exposed through request DTOs should match database limits.

Examples:

```txt
account.alias VARCHAR(80) -> @Size(max = 80)
customer.first_name VARCHAR(120) -> @Size(max = 120)
contact_point.contact_value VARCHAR(255) -> @Size(max = 255)
customer_status_history.reason VARCHAR(500) -> @Size(max = 500)
```

## Nested Validation

Nested request objects must use `@Valid`.

Example:

```txt
RegisterNaturalPersonCustomerRequest
  contactPoints: @Valid List<ContactPointRequest>
  addresses: @Valid List<AddressRequest>
```

## Practical Rule

When adding a column with `VARCHAR(n)`, add request validation with `@Size(max = n)` if that value can come from an API request.
