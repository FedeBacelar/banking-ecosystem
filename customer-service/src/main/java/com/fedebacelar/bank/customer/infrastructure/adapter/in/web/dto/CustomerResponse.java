package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
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
        List<ContactPointResponse> contactPoints,
        List<AddressResponse> addresses
) {
}
