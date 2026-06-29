package com.fedebacelar.bank.customer.application.usecase.registration;

import com.fedebacelar.bank.customer.application.command.RegisterNaturalPersonCustomerCommand;
import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.RegisterNaturalPersonCustomerUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerNumberGeneratorPort;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.port.out.IdentificationDocumentLookupPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.enums.CustomerStatus;
import com.fedebacelar.bank.customer.domain.enums.KycStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyLifecycleStatus;
import com.fedebacelar.bank.customer.domain.enums.PartyType;
import com.fedebacelar.bank.customer.domain.enums.RiskLevel;
import com.fedebacelar.bank.customer.domain.exception.DuplicateDocumentException;
import com.fedebacelar.bank.customer.domain.model.Address;
import com.fedebacelar.bank.customer.domain.model.ContactPoint;
import com.fedebacelar.bank.customer.domain.model.Customer;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.IdentificationDocument;
import com.fedebacelar.bank.customer.domain.model.KycProfile;
import com.fedebacelar.bank.customer.domain.model.NaturalPerson;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import com.fedebacelar.bank.customer.domain.model.Party;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RegisterNaturalPersonCustomerService implements RegisterNaturalPersonCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;
    private final IdentificationDocumentLookupPort identificationDocumentLookupPort;
    private final CustomerNumberGeneratorPort customerNumberGeneratorPort;
    private final Clock clock;

    public RegisterNaturalPersonCustomerService(
            CustomerRepositoryPort customerRepositoryPort,
            IdentificationDocumentLookupPort identificationDocumentLookupPort,
            CustomerNumberGeneratorPort customerNumberGeneratorPort,
            Clock clock
    ) {
        this.customerRepositoryPort = customerRepositoryPort;
        this.identificationDocumentLookupPort = identificationDocumentLookupPort;
        this.customerNumberGeneratorPort = customerNumberGeneratorPort;
        this.clock = clock;
    }

    @Override
    public CustomerDetails register(RegisterNaturalPersonCustomerCommand command) {
        if (identificationDocumentLookupPort.existsDocument(command.documentType(), command.documentNumber(), command.issuingCountry())) {
            throw new DuplicateDocumentException(command.documentType(), command.documentNumber(), command.issuingCountry());
        }

        Instant now = Instant.now(clock);
        UUID partyId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Party party = new Party(partyId, PartyType.NATURAL_PERSON, PartyLifecycleStatus.REGISTERED, now, now);

        NaturalPerson naturalPerson = new NaturalPerson(
                partyId,
                command.firstName(),
                command.middleName(),
                command.lastName(),
                command.birthDate(),
                command.nationality()
        );

        Customer customer = new Customer(
                customerId,
                partyId,
                customerNumberGeneratorPort.nextCustomerNumber(),
                CustomerStatus.PENDING_KYC,
                LocalDate.now(clock),
                null,
                now,
                now,
                null
        );

        IdentificationDocument document = new IdentificationDocument(
                UUID.randomUUID(),
                partyId,
                command.documentType(),
                command.documentNumber(),
                command.issuingCountry(),
                command.documentExpirationDate(),
                true
        );

        KycProfile kycProfile = new KycProfile(
                UUID.randomUUID(),
                customerId,
                RiskLevel.LOW,
                KycStatus.PENDING_REVIEW,
                null,
                null
        );

        List<ContactPoint> contactPoints = command.contactPoints().stream()
                .map(contact -> new ContactPoint(UUID.randomUUID(), partyId, contact.type(), contact.value(), false, true))
                .toList();

        List<Address> addresses = command.addresses().stream()
                .map(address -> new Address(
                        UUID.randomUUID(),
                        partyId,
                        address.type(),
                        address.street(),
                        address.streetNumber(),
                        address.city(),
                        address.province(),
                        address.postalCode(),
                        address.country()
                ))
                .toList();

        CustomerStatusHistory history = new CustomerStatusHistory(
                UUID.randomUUID(),
                customerId,
                null,
                CustomerStatus.PENDING_KYC,
                "Customer registered pending KYC review",
                now
        );

        NaturalPersonCustomer aggregate = new NaturalPersonCustomer(
                party,
                naturalPerson,
                customer,
                document,
                contactPoints,
                addresses,
                kycProfile,
                List.of(history)
        );

        return CustomerDetailsMapper.toDetails(customerRepositoryPort.save(aggregate));
    }
}
