package com.fedebacelar.bank.customer.application.port.out;

import com.fedebacelar.bank.customer.application.model.IdempotencyRecord;
import java.util.Optional;

public interface CustomerIdempotencyRepositoryPort {
    Optional<IdempotencyRecord> findByKey(String key);
    IdempotencyRecord save(IdempotencyRecord record);
}
