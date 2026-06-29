package com.fedebacelar.bank.customer.domain.model;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import java.time.LocalDate;
import java.util.UUID;

public record IdentificationDocument(
        UUID id,
        UUID partyId,
        DocumentType type,
        String number,
        String issuingCountry,
        LocalDate expirationDate,
        boolean primaryDocument
) {
}
