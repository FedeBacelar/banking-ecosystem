package com.fedebacelar.bank.customer.application.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record IdempotencyRecord(String idempotencyKey, String requestHash, UUID resourceId, Instant createdAt) {
    public static IdempotencyRecord pending(String idempotencyKey, String requestHash, Instant createdAt) {
        return new IdempotencyRecord(idempotencyKey, requestHash, null, createdAt);
    }

    public boolean completed() {
        return resourceId != null;
    }

    public IdempotencyRecord complete(UUID newResourceId) {
        return new IdempotencyRecord(idempotencyKey, requestHash,
                Objects.requireNonNull(newResourceId, "resourceId is required"), createdAt);
    }
}
