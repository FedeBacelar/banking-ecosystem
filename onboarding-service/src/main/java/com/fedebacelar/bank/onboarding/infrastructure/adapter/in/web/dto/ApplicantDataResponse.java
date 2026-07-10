package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
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
        ApplicantDocumentType documentType,
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
}
