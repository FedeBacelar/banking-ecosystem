package com.fedebacelar.bank.customer.application.port.out;

import com.fedebacelar.bank.customer.application.model.IdempotencyRecord;
import java.time.Instant;

public interface CustomerIdempotencyRepositoryPort {
    IdempotencyRecord acquire(String key, String requestHash, Instant now);
    IdempotencyRecord save(IdempotencyRecord record);
}
