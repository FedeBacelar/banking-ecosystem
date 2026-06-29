package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.IdentificationDocumentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentificationDocumentJpaRepository extends JpaRepository<IdentificationDocumentEntity, String> {

    boolean existsByDocumentTypeAndDocumentNumberAndIssuingCountry(DocumentType documentType, String documentNumber, String issuingCountry);

    Optional<IdentificationDocumentEntity> findByDocumentTypeAndDocumentNumberAndIssuingCountry(
            DocumentType documentType,
            String documentNumber,
            String issuingCountry
    );

    Optional<IdentificationDocumentEntity> findByPartyIdAndPrimaryDocumentTrue(String partyId);

    Optional<IdentificationDocumentEntity> findFirstByPartyId(String partyId);
}
