package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveCustomerKycRequest(
        @NotBlank @Size(max = 80) String reasonCode,
        @NotBlank @Size(max = 100) String changedBy
) {
}
