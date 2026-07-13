package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartOnboardingRequest(
        @NotBlank @Email @Size(max = 255) String email
) {
}
