package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OnboardingApplicantDataResponse(
        UUID applicationId,
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
        String country,
        Instant createdAt,
        Instant updatedAt
) {

    public static OnboardingApplicantDataResponse from(OnboardingApplicantData applicantData) {
        return new OnboardingApplicantDataResponse(
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
