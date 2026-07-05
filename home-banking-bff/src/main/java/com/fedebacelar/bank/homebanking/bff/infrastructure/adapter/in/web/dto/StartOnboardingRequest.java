package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StartOnboardingRequest(
        @NotBlank @Email String email
) {
}
