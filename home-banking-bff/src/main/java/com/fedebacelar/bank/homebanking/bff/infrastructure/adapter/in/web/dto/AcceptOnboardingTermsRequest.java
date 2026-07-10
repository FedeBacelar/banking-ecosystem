package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptOnboardingTermsRequest(
        @AssertTrue boolean accepted,
        @NotBlank @Size(max = 80) String termsVersion
) {
}
