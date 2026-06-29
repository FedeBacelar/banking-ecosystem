package com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountReasonRequest(
        @NotBlank @Size(max = 500) String reason,
        @NotBlank @Size(max = 120) String changedBy
) {
}
