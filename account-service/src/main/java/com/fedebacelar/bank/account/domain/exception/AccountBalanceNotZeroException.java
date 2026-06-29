package com.fedebacelar.bank.account.domain.exception;

import java.util.UUID;

public class AccountBalanceNotZeroException extends RuntimeException {

    public AccountBalanceNotZeroException(UUID accountId) {
        super("Account cannot be closed with non-zero balance: " + accountId);
    }
}
