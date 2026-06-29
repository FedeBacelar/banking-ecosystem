package com.fedebacelar.bank.customer.application.view;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerDetails(
        UUID customerId,
        UUID partyId,
        String customerNumber,
        CustomerStatus status,
        String firstName,
        String middleName,
        String lastName,
        LocalDate birthDate,
        String nationality,
        DocumentType documentType,
        String documentNumber,
        String issuingCountry,
        KycStatus kycStatus,
        RiskLevel riskLevel,
        List<ContactPointDetails> contactPoints,
        List<AddressDetails> addresses
) {
}
