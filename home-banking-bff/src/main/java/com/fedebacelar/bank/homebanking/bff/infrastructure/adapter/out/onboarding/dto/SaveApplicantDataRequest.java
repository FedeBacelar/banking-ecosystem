package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import java.time.LocalDate;

public record SaveApplicantDataRequest(
        String continuationToken,
        String firstName,
        String middleName,
        String lastName,
        LocalDate birthDate,
        String nationality,
        String documentType,
        String documentNumber,
        String documentIssuingCountry,
        LocalDate documentExpirationDate,
        String phoneNumber,
        String street,
        String streetNumber,
        String city,
        String province,
        String postalCode,
        String country
) {

    public static SaveApplicantDataRequest from(String continuationToken, OnboardingApplicantData applicantData) {
        return new SaveApplicantDataRequest(
                continuationToken,
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
                applicantData.country()
        );
    }
}
