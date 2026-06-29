package com.fedebacelar.bank.customer.domain.exception;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;

public class InvalidCustomerStatusTransitionException extends RuntimeException {

    public InvalidCustomerStatusTransitionException(CustomerStatus currentStatus, CustomerStatus requestedStatus) {
        super("Invalid customer status transition: " + currentStatus + " -> " + requestedStatus);
    }
}
