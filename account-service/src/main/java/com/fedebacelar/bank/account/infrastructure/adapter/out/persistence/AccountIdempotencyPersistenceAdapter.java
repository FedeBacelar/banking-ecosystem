package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import com.fedebacelar.bank.account.application.port.out.AccountIdempotencyRepositoryPort;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountIdempotencyEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountIdempotencyJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountIdempotencyPersistenceAdapter implements AccountIdempotencyRepositoryPort {
    private final AccountIdempotencyJpaRepository repository;
    public AccountIdempotencyPersistenceAdapter(AccountIdempotencyJpaRepository repository) { this.repository = repository; }
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyRecord acquire(String key, String requestHash, Instant now) {
        repository.insertIfAbsent(key, requestHash, now);
        return repository.findByKeyForUpdate(key)
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("Could not acquire account idempotency key."));
    }
    @Override public IdempotencyRecord save(IdempotencyRecord record) {
        AccountIdempotencyEntity entity = new AccountIdempotencyEntity();
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setRequestHash(record.requestHash());
        entity.setResourceId(record.resourceId() == null ? null : record.resourceId().toString());
        entity.setCreatedAt(record.createdAt());
        return toDomain(repository.save(entity));
    }
    private IdempotencyRecord toDomain(AccountIdempotencyEntity entity) {
        UUID resourceId = entity.getResourceId() == null ? null : UUID.fromString(entity.getResourceId());
        return new IdempotencyRecord(entity.getIdempotencyKey(), entity.getRequestHash(), resourceId, entity.getCreatedAt());
    }
}
