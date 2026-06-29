# Error Handling

Services use `ProblemDetail` for API errors.

The goal is to make errors predictable and useful for clients.

## Current Error Types

Common examples:

```txt
Invalid request -> 400
Resource not found -> 404
Business conflict -> 409
External service unavailable -> 503
```

## Validation Errors

Invalid request bodies return:

```txt
400 Bad Request
title: Invalid request
```

Where possible, field validation errors are included in the `errors` property.

## Malformed JSON Or Invalid Enum

Malformed JSON, invalid enum values, or unreadable request bodies return:

```txt
400 Bad Request
title: Invalid request
detail: Request body is malformed or contains invalid values
```

## Optimistic Locking

Concurrent updates return:

```txt
409 Conflict
```

The message asks the caller to retry with the latest state.

## Service-Specific Business Conflicts

Examples:

```txt
Duplicate document.
Invalid customer status transition.
Duplicate account alias.
Invalid account status transition.
Account cannot be closed with non-zero balance.
```
