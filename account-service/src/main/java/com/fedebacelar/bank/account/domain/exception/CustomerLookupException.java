package com.fedebacelar.bank.account.domain.exception;

import java.util.UUID;

public class CustomerLookupException extends RuntimeException {

    public CustomerLookupException(UUID customerId) {
        super("Customer service unavailable while looking up customer: " + customerId);
    }
}
