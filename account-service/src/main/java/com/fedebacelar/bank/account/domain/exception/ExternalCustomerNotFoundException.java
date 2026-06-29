package com.fedebacelar.bank.account.domain.exception;

import java.util.UUID;

public class ExternalCustomerNotFoundException extends RuntimeException {

    public ExternalCustomerNotFoundException(UUID customerId) {
        super("Customer not found: " + customerId);
    }
}
