package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartyJpaRepository extends JpaRepository<PartyEntity, String> {
}
