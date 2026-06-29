package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotNull AddressType type,
        @NotBlank @Size(max = 160) String street,
        @NotBlank @Size(max = 40) String streetNumber,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String province,
        @NotBlank @Size(max = 30) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$") String country
) {
}
