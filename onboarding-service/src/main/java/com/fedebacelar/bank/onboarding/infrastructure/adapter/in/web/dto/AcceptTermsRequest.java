package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptTermsRequest(
        @NotBlank @Size(max = 500) String continuationToken,
        @AssertTrue boolean accepted,
        @NotBlank @Size(max = 80) String termsVersion
) {
}
