package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.customer.application.model.IdempotencyRecord;
import com.fedebacelar.bank.customer.application.port.out.CustomerIdempotencyRepositoryPort;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerIdempotencyEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.CustomerIdempotencyJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerIdempotencyPersistenceAdapter implements CustomerIdempotencyRepositoryPort {
    private final CustomerIdempotencyJpaRepository repository;
    public CustomerIdempotencyPersistenceAdapter(CustomerIdempotencyJpaRepository repository) { this.repository = repository; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyRecord acquire(String key, String requestHash, Instant now) {
        repository.insertIfAbsent(key, requestHash, now);
        return repository.findByKeyForUpdate(key)
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("Could not acquire customer idempotency key."));
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        CustomerIdempotencyEntity entity = new CustomerIdempotencyEntity();
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setRequestHash(record.requestHash());
        entity.setResourceId(record.resourceId() == null ? null : record.resourceId().toString());
        entity.setCreatedAt(record.createdAt());
        return toDomain(repository.save(entity));
    }

    private IdempotencyRecord toDomain(CustomerIdempotencyEntity entity) {
        UUID resourceId = entity.getResourceId() == null ? null : UUID.fromString(entity.getResourceId());
        return new IdempotencyRecord(entity.getIdempotencyKey(), entity.getRequestHash(), resourceId, entity.getCreatedAt());
    }
}
