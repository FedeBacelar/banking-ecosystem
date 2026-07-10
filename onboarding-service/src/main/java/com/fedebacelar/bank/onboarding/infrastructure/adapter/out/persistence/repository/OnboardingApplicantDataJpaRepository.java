package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicantDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingApplicantDataJpaRepository extends JpaRepository<OnboardingApplicantDataEntity, String> {

    @Query(value = """
            SELECT COUNT(*)
              FROM onboarding_applicant_data d
              JOIN onboarding_application a ON a.id = d.application_id
             WHERE d.application_id <> :applicationId
               AND d.document_type = :documentType
               AND d.document_number = :documentNumber
               AND d.document_issuing_country = :issuingCountry
               AND a.status NOT IN ('COMPLETED', 'REJECTED', 'EXPIRED', 'CANCELLED')
            """, nativeQuery = true)
    long countActiveApplicationByDocumentExcluding(
            @Param("applicationId") String applicationId,
            @Param("documentType") String documentType,
            @Param("documentNumber") String documentNumber,
            @Param("issuingCountry") String issuingCountry
    );
}
