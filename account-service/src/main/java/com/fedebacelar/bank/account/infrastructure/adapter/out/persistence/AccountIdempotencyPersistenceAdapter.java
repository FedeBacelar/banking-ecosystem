package com.fedebacelar.bank.account.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.account.application.model.IdempotencyRecord;
import com.fedebacelar.bank.account.application.port.out.AccountIdempotencyRepositoryPort;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.entity.AccountIdempotencyEntity;
import com.fedebacelar.bank.account.infrastructure.adapter.out.persistence.repository.AccountIdempotencyJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountIdempotencyPersistenceAdapter implements AccountIdempotencyRepositoryPort {
    private final AccountIdempotencyJpaRepository repository;
    public AccountIdempotencyPersistenceAdapter(AccountIdempotencyJpaRepository repository) { this.repository = repository; }
    @Override public Optional<IdempotencyRecord> findByKey(String key) { return repository.findById(key).map(this::toDomain); }
    @Override public IdempotencyRecord save(IdempotencyRecord record) {
        AccountIdempotencyEntity entity = new AccountIdempotencyEntity();
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setRequestHash(record.requestHash());
        entity.setResourceId(record.resourceId().toString());
        entity.setCreatedAt(record.createdAt());
        return toDomain(repository.save(entity));
    }
    private IdempotencyRecord toDomain(AccountIdempotencyEntity entity) {
        return new IdempotencyRecord(entity.getIdempotencyKey(), entity.getRequestHash(), UUID.fromString(entity.getResourceId()), entity.getCreatedAt());
    }
}
