package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsumeOnboardingMagicLinkRequest(
        @NotBlank String token
) {
}
