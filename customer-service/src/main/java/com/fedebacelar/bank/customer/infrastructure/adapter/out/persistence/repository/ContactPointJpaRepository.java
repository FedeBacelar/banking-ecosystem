package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.ContactPointEntity;
import com.fedebacelar.bank.customer.domain.enums.ContactType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactPointJpaRepository extends JpaRepository<ContactPointEntity, String> {

    List<ContactPointEntity> findByPartyId(String partyId);

    Optional<ContactPointEntity> findFirstByContactTypeAndContactValueIgnoreCase(ContactType contactType, String contactValue);
}
