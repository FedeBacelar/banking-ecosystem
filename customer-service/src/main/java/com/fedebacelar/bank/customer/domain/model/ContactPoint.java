package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.ContactType;
import java.util.UUID;

public record ContactPoint(
        UUID id,
        UUID partyId,
        ContactType type,
        String value,
        boolean verified,
        boolean primaryContact
) {
}
