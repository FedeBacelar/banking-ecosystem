package com.fedebacelar.bank.account.application.mapper;

import com.fedebacelar.bank.account.application.view.AccountBalanceDetails;
import com.fedebacelar.bank.account.application.view.AccountDetails;
import com.fedebacelar.bank.account.domain.model.AccountBalance;
import com.fedebacelar.bank.account.domain.model.BankAccount;

public final class AccountDetailsMapper {

    private AccountDetailsMapper() {
    }

    public static AccountDetails toDetails(BankAccount aggregate) {
        var account = aggregate.account();
        return new AccountDetails(
                account.id(),
                account.customerId(),
                account.accountNumber(),
                account.cbu(),
                account.alias(),
                account.type(),
                account.currency(),
                account.status(),
                account.openedAt(),
                account.closedAt(),
                toDetails(aggregate.balance())
        );
    }

    public static AccountBalanceDetails toDetails(AccountBalance balance) {
        return new AccountBalanceDetails(
                balance.accountId(),
                balance.currency(),
                balance.currentBalance(),
                balance.availableBalance(),
                balance.holdBalance(),
                balance.updatedAt()
        );
    }
}
