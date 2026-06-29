package com.fedebacelar.bank.account.domain.exception;

import com.fedebacelar.bank.account.domain.enums.AccountStatus;
import java.util.UUID;

public class InvalidAccountStatusTransitionException extends RuntimeException {

    public InvalidAccountStatusTransitionException(UUID accountId, AccountStatus currentStatus, AccountStatus targetStatus) {
        super("Invalid account status transition for account " + accountId + ": " + currentStatus + " -> " + targetStatus);
    }
}
