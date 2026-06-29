package com.fedebacelar.bank.account.domain.model;

import com.fedebacelar.bank.account.domain.enums.CustomerStatus;
import java.util.UUID;

public record CustomerRef(
        UUID customerId,
        String customerNumber,
        CustomerStatus status
) {

    public boolean canOpenAccount() {
        return status == CustomerStatus.ACTIVE;
    }
}
