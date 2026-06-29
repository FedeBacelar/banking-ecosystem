package com.fedebacelar.bank.account.domain.model;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountStatusHistory(
        UUID id,
        UUID accountId,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
