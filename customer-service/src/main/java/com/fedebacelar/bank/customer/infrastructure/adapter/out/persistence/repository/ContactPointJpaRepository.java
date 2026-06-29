package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.ContactPointEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactPointJpaRepository extends JpaRepository<ContactPointEntity, String> {

    List<ContactPointEntity> findByPartyId(String partyId);
}
