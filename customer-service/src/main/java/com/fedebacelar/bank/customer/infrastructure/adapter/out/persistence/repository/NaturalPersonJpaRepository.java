package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.NaturalPersonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NaturalPersonJpaRepository extends JpaRepository<NaturalPersonEntity, String> {
}
