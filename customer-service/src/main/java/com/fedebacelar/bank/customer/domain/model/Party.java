package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
import java.time.Instant;
import java.util.UUID;

public record Party(
        UUID id,
        PartyType type,
        PartyLifecycleStatus lifecycleStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
