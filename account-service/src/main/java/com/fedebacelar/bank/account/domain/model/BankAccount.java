package com.fedebacelar.bank.account.domain.model;

import java.util.List;

public record BankAccount(
        Account account,
        AccountBalance balance,
        List<AccountStatusHistory> statusHistory
) {
}
