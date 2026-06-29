package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

public record CustomerStatusHistoryResponse(
        UUID id,
        UUID customerId,
        CustomerStatus previousStatus,
        CustomerStatus newStatus,
        String reason,
        Instant changedAt
) {
}
