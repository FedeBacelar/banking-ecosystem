package com.fedebacelar.bank.customer.application.command;

import com.fedebacelar.bank.customer.domain.enums.ContactType;

public record ContactPointCommand(
        ContactType type,
        String value,
        boolean verified
) {
    public ContactPointCommand(ContactType type, String value) {
        this(type, value, false);
    }
}
