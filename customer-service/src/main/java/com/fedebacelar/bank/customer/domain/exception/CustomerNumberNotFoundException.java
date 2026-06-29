package com.fedebacelar.bank.customer.domain.exception;

public class CustomerNumberNotFoundException extends RuntimeException {

    public CustomerNumberNotFoundException(String customerNumber) {
        super("Customer not found for customer number: " + customerNumber);
    }
}
