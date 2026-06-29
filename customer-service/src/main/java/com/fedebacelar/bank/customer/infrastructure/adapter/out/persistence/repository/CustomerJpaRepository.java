package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, String> {

    Optional<CustomerEntity> findByPartyId(String partyId);

    Optional<CustomerEntity> findByCustomerNumber(String customerNumber);
}
