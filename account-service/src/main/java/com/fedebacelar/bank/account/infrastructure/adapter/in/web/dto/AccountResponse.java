package com.fedebacelar.bank.account.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        UUID customerId,
        String accountNumber,
        String cbu,
        String alias,
        AccountType type,
        CurrencyCode currency,
        AccountStatus status,
        Instant openedAt,
        Instant closedAt,
        AccountBalanceResponse balance
) {
}
