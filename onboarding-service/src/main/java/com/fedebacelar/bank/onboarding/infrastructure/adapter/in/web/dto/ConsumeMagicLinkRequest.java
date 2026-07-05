package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsumeMagicLinkRequest(
        @NotBlank @Size(max = 500) String token
) {
}
