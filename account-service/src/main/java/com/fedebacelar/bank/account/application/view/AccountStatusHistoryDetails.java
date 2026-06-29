package com.fedebacelar.bank.account.application.view;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import java.time.Instant;
import java.util.UUID;

public record AccountStatusHistoryDetails(
        UUID accountId,
        AccountStatus previousStatus,
        AccountStatus newStatus,
        String reason,
        String changedBy,
        Instant changedAt
) {
}
