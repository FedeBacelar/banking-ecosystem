package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicantDataResponse(
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

    public OnboardingApplicantData toDomain() {
        return new OnboardingApplicantData(
                applicationId,
                firstName,
                middleName,
                lastName,
                birthDate,
                nationality,
                documentType,
                documentNumber,
                documentIssuingCountry,
                documentExpirationDate,
                phoneNumber,
                street,
                streetNumber,
                city,
                province,
                postalCode,
                country,
                createdAt,
                updatedAt
        );
    }
}
