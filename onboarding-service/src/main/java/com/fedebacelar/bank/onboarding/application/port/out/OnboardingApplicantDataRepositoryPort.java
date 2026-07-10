package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingApplicantDataRepositoryPort {

    ApplicantData save(ApplicantData applicantData);

    Optional<ApplicantData> findByApplicationId(UUID applicationId);

    boolean existsActiveApplicationByDocumentExcluding(
            UUID applicationId,
            String documentType,
            String documentNumber,
            String issuingCountry
    );
}
