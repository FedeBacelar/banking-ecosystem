package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.customer.domain.model.Address;
import com.fedebacelar.bank.customer.domain.model.ContactPoint;
import com.fedebacelar.bank.customer.domain.model.Customer;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.IdentificationDocument;
import com.fedebacelar.bank.customer.domain.model.DocumentNumber;
import com.fedebacelar.bank.customer.domain.model.KycProfile;
import com.fedebacelar.bank.customer.domain.model.NaturalPerson;
import com.fedebacelar.bank.customer.domain.model.Party;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.AddressEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.ContactPointEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerStatusHistoryEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.IdentificationDocumentEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.KycProfileEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.NaturalPersonEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.PartyEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerPersistenceMapper {

    public PartyEntity toEntity(Party party) {
        PartyEntity entity = new PartyEntity();
        entity.setId(party.id().toString());
        entity.setPartyType(party.type());
        entity.setLifecycleStatus(party.lifecycleStatus());
        entity.setCreatedAt(party.createdAt());
        entity.setUpdatedAt(party.updatedAt());
        return entity;
    }

    public Party toDomain(PartyEntity entity) {
        return new Party(UUID.fromString(entity.getId()), entity.getPartyType(), entity.getLifecycleStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public NaturalPersonEntity toEntity(NaturalPerson naturalPerson) {
        NaturalPersonEntity entity = new NaturalPersonEntity();
        entity.setPartyId(naturalPerson.partyId().toString());
        entity.setFirstName(naturalPerson.firstName());
        entity.setMiddleName(naturalPerson.middleName());
        entity.setLastName(naturalPerson.lastName());
        entity.setBirthDate(naturalPerson.birthDate());
        entity.setNationality(naturalPerson.nationality());
        return entity;
    }

    public NaturalPerson toDomain(NaturalPersonEntity entity) {
        return new NaturalPerson(UUID.fromString(entity.getPartyId()), entity.getFirstName(), entity.getMiddleName(), entity.getLastName(), entity.getBirthDate(), entity.getNationality());
    }

    public CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.id().toString());
        entity.setPartyId(customer.partyId().toString());
        entity.setCustomerNumber(customer.customerNumber());
        entity.setStatus(customer.status());
        entity.setOnboardingDate(customer.onboardingDate());
        entity.setClosedAt(customer.closedAt());
        entity.setCreatedAt(customer.createdAt());
        entity.setUpdatedAt(customer.updatedAt());
        entity.setVersion(customer.version());
        return entity;
    }

    public Customer toDomain(CustomerEntity entity) {
        return new Customer(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getPartyId()),
                entity.getCustomerNumber(),
                entity.getStatus(),
                entity.getOnboardingDate(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    public IdentificationDocumentEntity toEntity(IdentificationDocument document) {
        IdentificationDocumentEntity entity = new IdentificationDocumentEntity();
        entity.setId(document.id().toString());
        entity.setPartyId(document.partyId().toString());
        entity.setDocumentType(document.type());
        entity.setDocumentNumber(document.number());
        entity.setDocumentNumberCanonical(DocumentNumber.canonical(document.number()));
        entity.setIssuingCountry(document.issuingCountry());
        entity.setExpirationDate(document.expirationDate());
        entity.setPrimaryDocument(document.primaryDocument());
        return entity;
    }

    public IdentificationDocument toDomain(IdentificationDocumentEntity entity) {
        return new IdentificationDocument(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getPartyId()),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getIssuingCountry(),
                entity.getExpirationDate(),
                entity.isPrimaryDocument()
        );
    }

    public ContactPointEntity toEntity(ContactPoint contactPoint) {
        ContactPointEntity entity = new ContactPointEntity();
        entity.setId(contactPoint.id().toString());
        entity.setPartyId(contactPoint.partyId().toString());
        entity.setContactType(contactPoint.type());
        entity.setContactValue(contactPoint.value());
        entity.setVerified(contactPoint.verified());
        entity.setPrimaryContact(contactPoint.primaryContact());
        return entity;
    }

    public ContactPoint toDomain(ContactPointEntity entity) {
        return new ContactPoint(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getPartyId()),
                entity.getContactType(),
                entity.getContactValue(),
                entity.isVerified(),
                entity.isPrimaryContact()
        );
    }

    public AddressEntity toEntity(Address address) {
        AddressEntity entity = new AddressEntity();
        entity.setId(address.id().toString());
        entity.setPartyId(address.partyId().toString());
        entity.setAddressType(address.type());
        entity.setStreet(address.street());
        entity.setStreetNumber(address.streetNumber());
        entity.setCity(address.city());
        entity.setProvince(address.province());
        entity.setPostalCode(address.postalCode());
        entity.setCountry(address.country());
        return entity;
    }

    public Address toDomain(AddressEntity entity) {
        return new Address(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getPartyId()),
                entity.getAddressType(),
                entity.getStreet(),
                entity.getStreetNumber(),
                entity.getCity(),
                entity.getProvince(),
                entity.getPostalCode(),
                entity.getCountry()
        );
    }

    public KycProfileEntity toEntity(KycProfile kycProfile) {
        KycProfileEntity entity = new KycProfileEntity();
        entity.setId(kycProfile.id().toString());
        entity.setCustomerId(kycProfile.customerId().toString());
        entity.setRiskLevel(kycProfile.riskLevel());
        entity.setKycStatus(kycProfile.status());
        entity.setLastReviewAt(kycProfile.lastReviewAt());
        entity.setNextReviewAt(kycProfile.nextReviewAt());
        return entity;
    }

    public KycProfile toDomain(KycProfileEntity entity) {
        return new KycProfile(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getCustomerId()),
                entity.getRiskLevel(),
                entity.getKycStatus(),
                entity.getLastReviewAt(),
                entity.getNextReviewAt()
        );
    }

    public CustomerStatusHistoryEntity toEntity(CustomerStatusHistory history) {
        CustomerStatusHistoryEntity entity = new CustomerStatusHistoryEntity();
        entity.setId(history.id().toString());
        entity.setCustomerId(history.customerId().toString());
        entity.setPreviousStatus(history.previousStatus());
        entity.setNewStatus(history.newStatus());
        entity.setReason(history.reason());
        entity.setChangedBy(history.changedBy());
        entity.setChangedAt(history.changedAt());
        return entity;
    }

    public CustomerStatusHistory toDomain(CustomerStatusHistoryEntity entity) {
        return new CustomerStatusHistory(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getCustomerId()),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getReason(),
                entity.getChangedBy(),
                entity.getChangedAt()
        );
    }
}
