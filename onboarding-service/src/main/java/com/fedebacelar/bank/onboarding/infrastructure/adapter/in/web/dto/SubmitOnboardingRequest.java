package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitOnboardingRequest(@NotBlank String continuationToken) {
}
