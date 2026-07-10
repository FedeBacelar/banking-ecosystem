package com.fedebacelar.bank.account.application.model;

import java.time.Instant;
import java.util.UUID;

public record IdempotencyRecord(String idempotencyKey, String requestHash, UUID resourceId, Instant createdAt) {
}
