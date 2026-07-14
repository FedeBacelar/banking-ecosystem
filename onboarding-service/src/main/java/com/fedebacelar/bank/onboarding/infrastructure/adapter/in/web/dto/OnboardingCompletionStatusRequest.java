package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OnboardingCompletionStatusRequest(
        @NotBlank @Size(max = 255) String keycloakSubject
) {
}
