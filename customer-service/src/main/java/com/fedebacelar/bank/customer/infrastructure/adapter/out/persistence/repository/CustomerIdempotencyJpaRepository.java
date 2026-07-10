package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerIdempotencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerIdempotencyJpaRepository extends JpaRepository<CustomerIdempotencyEntity, String> {
}
