package com.fedebacelar.bank.account.domain.exception;

public class DuplicateAccountAliasException extends RuntimeException {

    public DuplicateAccountAliasException(String alias) {
        super("Account alias already registered: " + alias);
    }
}
