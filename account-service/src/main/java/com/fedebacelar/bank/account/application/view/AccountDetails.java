package com.fedebacelar.bank.account.application.view;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import java.time.Instant;
import java.util.UUID;

public record AccountDetails(
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
        AccountBalanceDetails balance
) {
}
