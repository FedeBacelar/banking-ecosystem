package com.fedebacelar.bank.homebanking.bff.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OnboardingApplicantData(
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
}
