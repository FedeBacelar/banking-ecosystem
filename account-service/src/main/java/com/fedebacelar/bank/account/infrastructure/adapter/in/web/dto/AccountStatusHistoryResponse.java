package com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountStatusHistoryResponse(
        UUID accountId,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
