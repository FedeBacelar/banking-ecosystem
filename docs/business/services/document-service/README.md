# document-service

`document-service` owns document metadata and file storage integration for banking evidence.

## Business Capability

The service provides a controlled place to receive files required by business processes.

For onboarding, the initial document categories are:

```txt
DNI_FRONT
DNI_BACK
```

## Ownership

The service owns:

```txt
- Document metadata.
- Document category.
- Original file name.
- Content type and size.
- Object storage location.
- Storage status.
```

The service does not own:

```txt
- Onboarding application state.
- Manual or automatic review decisions.
- Customer master data.
- Account opening.
- Authentication users.
- Public document viewing.
```

## Current Business Rule

A document upload stores the file in object storage and records metadata with status `STORED`.

Review and approval steps belong to onboarding and will be added later as explicit workflow states or tasks.
