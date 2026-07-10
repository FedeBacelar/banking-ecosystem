package com.fedebacelar.bank.onboarding.application.mapper;

import com.fedebacelar.bank.onboarding.application.view.ApplicantDataDetails;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;

public final class ApplicantDataDetailsMapper {

    private ApplicantDataDetailsMapper() {
    }

    public static ApplicantDataDetails toDetails(ApplicantData applicantData) {
        return new ApplicantDataDetails(
                applicantData.applicationId(),
                applicantData.firstName(),
                applicantData.middleName(),
                applicantData.lastName(),
                applicantData.birthDate(),
                applicantData.nationality(),
                applicantData.documentType(),
                applicantData.documentNumber(),
                applicantData.documentIssuingCountry(),
                applicantData.documentExpirationDate(),
                applicantData.phoneNumber(),
                applicantData.street(),
                applicantData.streetNumber(),
                applicantData.city(),
                applicantData.province(),
                applicantData.postalCode(),
                applicantData.country(),
                applicantData.createdAt(),
                applicantData.updatedAt()
        );
    }
}
