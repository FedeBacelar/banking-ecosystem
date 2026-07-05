package com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.document.infrastructure.adapter.out.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, String> {
}

