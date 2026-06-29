package com.fedebacelar.bank.customer.application.view;

import com.fedebacelar.bank.customer.domain.enums.ContactType;
import java.util.UUID;

public record ContactPointDetails(
        UUID id,
        ContactType type,
        String value,
        boolean verified,
        boolean primaryContact
) {
}
