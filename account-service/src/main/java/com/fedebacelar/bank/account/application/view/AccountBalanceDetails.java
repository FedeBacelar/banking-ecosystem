package com.fedebacelar.bank.account.application.view;

import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalanceDetails(
        UUID accountId,
        CurrencyCode currency,
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        BigDecimal holdBalance,
        Instant updatedAt
) {
}
