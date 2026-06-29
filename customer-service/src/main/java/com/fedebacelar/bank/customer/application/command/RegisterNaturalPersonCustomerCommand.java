package com.fedebacelar.bank.customer.application.command;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import java.time.LocalDate;
import java.util.List;

public record RegisterNaturalPersonCustomerCommand(
        String firstName,
        String middleName,
        String lastName,
        LocalDate birthDate,
        String nationality,
        DocumentType documentType,
        String documentNumber,
        String issuingCountry,
        LocalDate documentExpirationDate,
        List<ContactPointCommand> contactPoints,
        List<AddressCommand> addresses
) {
}
