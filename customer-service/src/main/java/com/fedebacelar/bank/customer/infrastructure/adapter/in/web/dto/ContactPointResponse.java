package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.ContactType;
import java.util.UUID;

public record ContactPointResponse(
        UUID id,
        ContactType type,
        String value,
        boolean verified,
        boolean primaryContact
) {
}
