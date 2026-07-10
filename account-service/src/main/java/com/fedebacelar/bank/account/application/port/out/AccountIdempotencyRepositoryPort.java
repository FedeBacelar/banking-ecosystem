package com.fedebacelar.bank.account.application.port.out;

import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import java.util.Optional;

public interface AccountIdempotencyRepositoryPort {
    Optional<IdempotencyRecord> findByKey(String key);
    IdempotencyRecord save(IdempotencyRecord record);
}
