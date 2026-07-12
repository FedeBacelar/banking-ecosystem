package com.fedebacelar.bank.account.application.port.out;

import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import java.time.Instant;

public interface AccountIdempotencyRepositoryPort {
    IdempotencyRecord acquire(String key, String requestHash, Instant now);
    IdempotencyRecord save(IdempotencyRecord record);
}
