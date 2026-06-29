package com.fedebacelar.bank.customer.application.view;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import java.time.Instant;
import java.util.UUID;

public record CustomerStatusHistoryDetails(
        UUID id,
        UUID customerId,
        CustomerStatus previousStatus,
        CustomerStatus newStatus,
        String reason,
        Instant changedAt
) {
}
