package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.ContactType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactPointRequest(
        @NotNull ContactType type,
        @NotBlank @Size(max = 255) String value
) {

    @AssertTrue(message = "email contact must contain a valid email address")
    public boolean isValidEmailContact() {
        return type != ContactType.EMAIL || value == null || value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    @AssertTrue(message = "phone contact must contain a valid phone number")
    public boolean isValidPhoneContact() {
        return type != ContactType.PHONE || value == null || value.matches("^\\+?[0-9][0-9\\s-]{6,24}$");
    }
}
