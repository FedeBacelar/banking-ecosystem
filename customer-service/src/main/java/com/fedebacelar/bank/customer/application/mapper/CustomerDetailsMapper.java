package com.fedebacelar.bank.customer.application.mapper;

import com.fedebacelar.bank.customer.application.view.AddressDetails;
import com.fedebacelar.bank.customer.application.view.ContactPointDetails;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import java.util.List;

public final class CustomerDetailsMapper {

    private CustomerDetailsMapper() {
    }

    public static CustomerDetails toDetails(NaturalPersonCustomer aggregate) {
        List<ContactPointDetails> contacts = aggregate.contactPoints().stream()
                .map(contact -> new ContactPointDetails(
                        contact.id(),
                        contact.type(),
                        contact.value(),
                        contact.verified(),
                        contact.primaryContact()
                ))
                .toList();

        List<AddressDetails> addresses = aggregate.addresses().stream()
                .map(address -> new AddressDetails(
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

        return new CustomerDetails(
                aggregate.customer().id(),
                aggregate.party().id(),
                aggregate.customer().customerNumber(),
                aggregate.customer().status(),
                aggregate.naturalPerson().firstName(),
                aggregate.naturalPerson().middleName(),
                aggregate.naturalPerson().lastName(),
                aggregate.naturalPerson().birthDate(),
                aggregate.naturalPerson().nationality(),
                aggregate.primaryDocument().type(),
                aggregate.primaryDocument().number(),
                aggregate.primaryDocument().issuingCountry(),
                aggregate.kycProfile().status(),
                aggregate.kycProfile().riskLevel(),
                contacts,
                addresses
        );
    }
}
