package com.fedebacelar.bank.account.application.command;

import com.fedebacelar.bank.account.domain.enums.AccountType;
import com.fedebacelar.bank.account.domain.enums.CurrencyCode;
import java.util.UUID;

public record OpenAccountCommand(
        UUID customerId,
        AccountType type,
        CurrencyCode currency,
        String alias
) {
}
