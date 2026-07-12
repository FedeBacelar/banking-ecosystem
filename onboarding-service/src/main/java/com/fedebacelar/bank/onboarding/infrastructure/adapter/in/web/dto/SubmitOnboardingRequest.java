package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SubmitOnboardingRequest(
        @NotBlank @Size(max = 500) String continuationToken,
        @NotBlank @Size(max = 120) String firstName,
        @Size(max = 120) String middleName,
        @NotBlank @Size(max = 120) String lastName,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String nationality,
        @NotNull ApplicantDocumentType documentType,
        @NotBlank @Size(max = 80) String documentNumber,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String documentIssuingCountry,
        LocalDate documentExpirationDate,
        @NotBlank @Pattern(regexp = "^\\+?[0-9][0-9\\s-]{6,24}$") String phoneNumber,
        @NotBlank @Size(max = 160) String street,
        @NotBlank @Size(max = 40) String streetNumber,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String province,
        @NotBlank @Size(max = 30) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String country,
        @AssertTrue boolean termsAccepted
) {
}
