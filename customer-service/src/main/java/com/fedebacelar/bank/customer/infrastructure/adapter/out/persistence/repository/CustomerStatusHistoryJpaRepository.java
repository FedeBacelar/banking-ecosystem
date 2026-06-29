package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerStatusHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerStatusHistoryJpaRepository extends JpaRepository<CustomerStatusHistoryEntity, String> {

    List<CustomerStatusHistoryEntity> findByCustomerIdOrderByChangedAtAsc(String customerId);
}
