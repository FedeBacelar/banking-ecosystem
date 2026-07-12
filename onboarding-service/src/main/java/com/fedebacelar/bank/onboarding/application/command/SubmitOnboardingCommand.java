package com.fedebacelar.bank.onboarding.application.command;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import java.time.LocalDate;

public record SubmitOnboardingCommand(
        String continuationToken,
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
        boolean termsAccepted,
        OnboardingDocumentUpload dniFront,
        OnboardingDocumentUpload dniBack
) {
}
