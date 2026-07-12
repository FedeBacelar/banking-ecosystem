package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.customer.application.command.AddressCommand;
import com.fedebacelar.bank.customer.application.command.CustomerReasonCommand;
import com.fedebacelar.bank.customer.application.command.ContactPointCommand;
import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.application.view.CustomerStatusHistoryDetails;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.AddressResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.ContactPointResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerReasonRequest;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.CustomerStatusHistoryResponse;
import com.fedebacelar.bank.customer.infrastructure.adapter.in.web.dto.RegisterNaturalPersonCustomerRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerWebMapper {

    public RegisterNaturalPersonCustomerCommand toCommand(RegisterNaturalPersonCustomerRequest request) {
        var contacts = request.contactPoints() == null ? List.<ContactPointCommand>of() : request.contactPoints().stream()
                .map(contact -> new ContactPointCommand(contact.type(), contact.value(), Boolean.TRUE.equals(contact.verified())))
                .toList();

        var addresses = request.addresses() == null ? List.<AddressCommand>of() : request.addresses().stream()
                .map(address -> new AddressCommand(
                        address.type(),
                        address.street(),
                        address.streetNumber(),
                        address.city(),
                        address.province(),
                        address.postalCode(),
                        address.country()
                ))
                .toList();

        return new RegisterNaturalPersonCustomerCommand(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.birthDate(),
                request.nationality(),
                request.documentType(),
                request.documentNumber(),
                request.issuingCountry(),
                request.documentExpirationDate(),
                contacts,
                addresses
        );
    }

    public CustomerReasonCommand toCommand(UUID customerId, CustomerReasonRequest request) {
        return new CustomerReasonCommand(customerId, request.reason());
    }

    public CustomerResponse toResponse(CustomerDetails details) {
        var contacts = details.contactPoints().stream()
                .map(contact -> new ContactPointResponse(contact.id(), contact.type(), contact.value(), contact.verified(), contact.primaryContact()))
                .toList();

        var addresses = details.addresses().stream()
                .map(address -> new AddressResponse(
                        address.id(),
                        address.type(),
                        address.street(),
                        address.streetNumber(),
                        address.city(),
                        address.province(),
                        address.postalCode(),
                        address.country()
                ))
                .toList();

        return new CustomerResponse(
                details.customerId(),
                details.partyId(),
                details.customerNumber(),
                details.status(),
                details.firstName(),
                details.middleName(),
                details.lastName(),
                details.birthDate(),
                details.nationality(),
                details.documentType(),
                details.documentNumber(),
                details.issuingCountry(),
                details.kycStatus(),
                details.riskLevel(),
                contacts,
                addresses
        );
    }

    public CustomerStatusHistoryResponse toResponse(CustomerStatusHistoryDetails details) {
        return new CustomerStatusHistoryResponse(
                details.id(),
                details.customerId(),
                details.previousStatus(),
                details.newStatus(),
                details.reason(),
                details.changedBy(),
                details.changedAt()
        );
    }
}
