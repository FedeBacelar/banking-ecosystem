package com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.entity.DocumentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, String> {
    Optional<DocumentEntity> findByIdempotencyKey(String idempotencyKey);
}

