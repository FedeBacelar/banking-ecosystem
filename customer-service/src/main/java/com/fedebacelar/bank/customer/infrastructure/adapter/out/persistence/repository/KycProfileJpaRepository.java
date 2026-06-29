package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.KycProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycProfileJpaRepository extends JpaRepository<KycProfileEntity, String> {

    Optional<KycProfileEntity> findByCustomerId(String customerId);
}
