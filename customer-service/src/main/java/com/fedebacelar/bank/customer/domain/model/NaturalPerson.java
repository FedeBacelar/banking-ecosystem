package com.fedebacelar.bank.customer.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record NaturalPerson(
        UUID partyId,
        String firstName,
        String middleName,
        String lastName,
        LocalDate birthDate,
        String nationality
) {
}
