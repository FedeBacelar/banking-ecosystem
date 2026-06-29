package com.fedebacelar.bank.account.domain.model;

import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountBalance(
        UUID id,
        UUID accountId,
        CurrencyCode currency,
        BigDecimal currentBalance,
        BigDecimal availableBalance,
        BigDecimal holdBalance,
        Instant updatedAt,
        Long version
) {

    public boolean isZero() {
        return currentBalance.compareTo(BigDecimal.ZERO) == 0
                && availableBalance.compareTo(BigDecimal.ZERO) == 0
                && holdBalance.compareTo(BigDecimal.ZERO) == 0;
    }
}
