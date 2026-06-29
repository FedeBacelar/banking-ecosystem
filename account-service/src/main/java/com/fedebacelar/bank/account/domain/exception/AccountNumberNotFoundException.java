package com.fedebacelar.bank.account.domain.exception;

public class AccountNumberNotFoundException extends RuntimeException {

    public AccountNumberNotFoundException(String accountNumber) {
        super("Account not found for account number: " + accountNumber);
    }
}
