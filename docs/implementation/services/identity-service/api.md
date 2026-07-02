# identity-service API

## Create Identity Link

```txt
POST /identity-links
```

Request:

```json
{
  "customerId": "22222222-2222-2222-2222-222222222222",
  "provider": "KEYCLOAK",
  "providerSubject": "keycloak-sub-1"
}
```

Response status:

```txt
201 Created
```

## Resolve Active Identity Link

```txt
GET /identity-links/providers/{provider}/subjects/{providerSubject}
```

Returns the active link for the authenticated provider subject.

If the link does not exist, the service returns `404`.

If the link exists but is not active, the service returns `409`.

## Get Customer Identity Links

```txt
GET /identity-links/customers/{customerId}
```

Returns all identity links for a customer.

## Activate Identity Link

```txt
PATCH /identity-links/{identityLinkId}/activate
```

## Disable Identity Link

```txt
PATCH /identity-links/{identityLinkId}/disable
```
