package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record RegisterNaturalPersonCustomerRequest(
        @NotBlank @Size(max = 120) String firstName,
        @Size(max = 120) String middleName,
        @NotBlank @Size(max = 120) String lastName,
        @NotNull @Past LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String nationality,
        @NotNull DocumentType documentType,
        @NotBlank @Size(max = 80) String documentNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String issuingCountry,
        LocalDate documentExpirationDate,
        @Valid List<ContactPointRequest> contactPoints,
        @Valid List<AddressRequest> addresses
) {
}
