package com.fedebacelar.bank.account.domain.exception;

public class AliasNotFoundException extends RuntimeException {

    public AliasNotFoundException(String alias) {
        super("Account not found for alias: " + alias);
    }
}
