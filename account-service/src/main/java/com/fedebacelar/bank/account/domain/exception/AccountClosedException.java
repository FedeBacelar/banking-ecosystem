package com.fedebacelar.bank.account.domain.exception;

import java.util.UUID;

public class AccountClosedException extends RuntimeException {

    public AccountClosedException(UUID accountId, String operation) {
        super("Cannot " + operation + " closed account " + accountId);
    }
}
