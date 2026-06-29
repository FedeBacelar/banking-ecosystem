package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerReasonRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
