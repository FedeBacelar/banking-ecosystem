package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Customer(
        UUID id,
        UUID partyId,
        String customerNumber,
        CustomerStatus status,
        LocalDate onboardingDate,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    public Customer withStatus(CustomerStatus newStatus, Instant changedAt) {
        Instant closedAtValue = newStatus == CustomerStatus.CLOSED ? changedAt : closedAt;
        return new Customer(id, partyId, customerNumber, newStatus, onboardingDate, closedAtValue, createdAt, changedAt, version);
    }
}
