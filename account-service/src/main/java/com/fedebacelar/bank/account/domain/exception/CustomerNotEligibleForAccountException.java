package com.fedebacelar.bank.account.domain.exception;

import com.fedebacelar.bank.account.domain.enums.CustomerStatus;
import java.util.UUID;

public class CustomerNotEligibleForAccountException extends RuntimeException {

    public CustomerNotEligibleForAccountException(UUID customerId, CustomerStatus status) {
        super("Customer " + customerId + " is not eligible for account opening. Current status: " + status);
    }
}
