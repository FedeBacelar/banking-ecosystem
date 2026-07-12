package com.fedebacelar.bank.customer.domain.exception;

public class CustomerEmailNotFoundException extends RuntimeException {
    public CustomerEmailNotFoundException(String email) {
        super("Customer email was not found: " + email);
    }
}
