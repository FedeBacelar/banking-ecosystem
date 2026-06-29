package com.fedebacelar.bank.account.infrastructure.adapter.out.customer.dto;

import com.fedebacelar.bank.account.domain.enums.CustomerStatus;
import java.util.UUID;

public record CustomerResponse(
        UUID customerId,
        String customerNumber,
        CustomerStatus status
) {
}
