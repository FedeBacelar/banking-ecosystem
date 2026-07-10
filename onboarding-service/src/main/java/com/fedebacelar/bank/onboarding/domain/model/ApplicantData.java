package com.fedebacelar.bank.onboarding.domain.model;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicantData(
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
        Instant updatedAt,
        long version
) {

    public static ApplicantData create(
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
            Instant now
    ) {
        return new ApplicantData(
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
                now,
                now,
                0L
        );
    }

    public ApplicantData updateFrom(
            ApplicantData source,
            Instant now
    ) {
        return new ApplicantData(
                applicationId,
                source.firstName(),
                source.middleName(),
                source.lastName(),
                source.birthDate(),
                source.nationality(),
                source.documentType(),
                source.documentNumber(),
                source.documentIssuingCountry(),
                source.documentExpirationDate(),
                source.phoneNumber(),
                source.street(),
                source.streetNumber(),
                source.city(),
                source.province(),
                source.postalCode(),
                source.country(),
                createdAt,
                now,
                version
        );
    }
}
