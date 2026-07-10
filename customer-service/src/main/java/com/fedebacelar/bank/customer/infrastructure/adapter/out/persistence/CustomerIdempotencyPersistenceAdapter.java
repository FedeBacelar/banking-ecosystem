package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.customer.application.model.IdempotencyRecord;
import com.fedebacelar.bank.customer.application.port.out.CustomerIdempotencyRepositoryPort;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerIdempotencyEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.CustomerIdempotencyJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerIdempotencyPersistenceAdapter implements CustomerIdempotencyRepositoryPort {
    private final CustomerIdempotencyJpaRepository repository;
    public CustomerIdempotencyPersistenceAdapter(CustomerIdempotencyJpaRepository repository) { this.repository = repository; }

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {
        return repository.findById(key).map(this::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        CustomerIdempotencyEntity entity = new CustomerIdempotencyEntity();
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setRequestHash(record.requestHash());
        entity.setResourceId(record.resourceId().toString());
        entity.setCreatedAt(record.createdAt());
        return toDomain(repository.save(entity));
    }

    private IdempotencyRecord toDomain(CustomerIdempotencyEntity entity) {
        return new IdempotencyRecord(entity.getIdempotencyKey(), entity.getRequestHash(), UUID.fromString(entity.getResourceId()), entity.getCreatedAt());
    }
}
