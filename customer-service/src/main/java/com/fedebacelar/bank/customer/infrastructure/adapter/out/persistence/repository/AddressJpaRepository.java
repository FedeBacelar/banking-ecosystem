package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.AddressEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressJpaRepository extends JpaRepository<AddressEntity, String> {

    List<AddressEntity> findByPartyId(String partyId);
}
