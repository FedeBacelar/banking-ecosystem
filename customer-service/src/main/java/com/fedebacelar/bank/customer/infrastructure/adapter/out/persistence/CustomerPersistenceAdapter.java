package com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.port.out.IdentificationDocumentLookupPort;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.enums.ContactType;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.DocumentNumber;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.mapper.CustomerPersistenceMapper;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.AddressJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.ContactPointJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.CustomerJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.CustomerStatusHistoryJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.IdentificationDocumentJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.KycProfileJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.NaturalPersonJpaRepository;
import com.fedebacelar.bank.customer.infrastructure.adapter.out.persistence.repository.PartyJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort, IdentificationDocumentLookupPort {

    private final PartyJpaRepository partyJpaRepository;
    private final NaturalPersonJpaRepository naturalPersonJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;
    private final IdentificationDocumentJpaRepository identificationDocumentJpaRepository;
    private final ContactPointJpaRepository contactPointJpaRepository;
    private final AddressJpaRepository addressJpaRepository;
    private final KycProfileJpaRepository kycProfileJpaRepository;
    private final CustomerStatusHistoryJpaRepository customerStatusHistoryJpaRepository;
    private final CustomerPersistenceMapper mapper;

    public CustomerPersistenceAdapter(
            PartyJpaRepository partyJpaRepository,
            NaturalPersonJpaRepository naturalPersonJpaRepository,
            CustomerJpaRepository customerJpaRepository,
            IdentificationDocumentJpaRepository identificationDocumentJpaRepository,
            ContactPointJpaRepository contactPointJpaRepository,
            AddressJpaRepository addressJpaRepository,
            KycProfileJpaRepository kycProfileJpaRepository,
            CustomerStatusHistoryJpaRepository customerStatusHistoryJpaRepository,
            CustomerPersistenceMapper mapper
    ) {
        this.partyJpaRepository = partyJpaRepository;
        this.naturalPersonJpaRepository = naturalPersonJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
        this.identificationDocumentJpaRepository = identificationDocumentJpaRepository;
        this.contactPointJpaRepository = contactPointJpaRepository;
        this.addressJpaRepository = addressJpaRepository;
        this.kycProfileJpaRepository = kycProfileJpaRepository;
        this.customerStatusHistoryJpaRepository = customerStatusHistoryJpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public NaturalPersonCustomer save(NaturalPersonCustomer customer) {
        partyJpaRepository.save(mapper.toEntity(customer.party()));
        naturalPersonJpaRepository.save(mapper.toEntity(customer.naturalPerson()));
        CustomerEntity customerEntity = customerJpaRepository.save(mapper.toEntity(customer.customer()));
        identificationDocumentJpaRepository.save(mapper.toEntity(customer.primaryDocument()));
        contactPointJpaRepository.saveAll(customer.contactPoints().stream().map(mapper::toEntity).toList());
        addressJpaRepository.saveAll(customer.addresses().stream().map(mapper::toEntity).toList());
        kycProfileJpaRepository.save(mapper.toEntity(customer.kycProfile()));
        customerStatusHistoryJpaRepository.saveAll(customer.statusHistory().stream().map(mapper::toEntity).toList());
        return assemble(customerEntity)
                .orElseThrow(() -> new IllegalStateException("Saved customer aggregate could not be reassembled: " + customerEntity.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NaturalPersonCustomer> findByCustomerId(UUID customerId) {
        return customerJpaRepository.findById(customerId.toString()).flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NaturalPersonCustomer> findByDocument(DocumentType type, String number, String country) {
        return identificationDocumentJpaRepository.findByDocumentTypeAndDocumentNumberCanonicalAndIssuingCountry(
                        type, DocumentNumber.canonical(number), country.toUpperCase(java.util.Locale.ROOT)
                )
                .flatMap(document -> customerJpaRepository.findByPartyId(document.getPartyId()))
                .flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NaturalPersonCustomer> findByEmail(String email) {
        return contactPointJpaRepository.findFirstByContactTypeAndContactValueIgnoreCase(ContactType.EMAIL, email)
                .flatMap(contact -> customerJpaRepository.findByPartyId(contact.getPartyId()))
                .flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NaturalPersonCustomer> findByCustomerNumber(String customerNumber) {
        return customerJpaRepository.findByCustomerNumber(customerNumber).flatMap(this::assemble);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerStatusHistory> findStatusHistory(UUID customerId) {
        return customerStatusHistoryJpaRepository.findByCustomerIdOrderByChangedAtAsc(customerId.toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsDocument(DocumentType type, String number, String country) {
        return identificationDocumentJpaRepository.existsByDocumentTypeAndDocumentNumberCanonicalAndIssuingCountry(
                type, DocumentNumber.canonical(number), country.toUpperCase(java.util.Locale.ROOT)
        );
    }

    private Optional<NaturalPersonCustomer> assemble(CustomerEntity customerEntity) {
        String partyId = customerEntity.getPartyId();
        String customerId = customerEntity.getId();
        var party = partyJpaRepository.findById(partyId);
        var naturalPerson = naturalPersonJpaRepository.findById(partyId);
        var document = identificationDocumentJpaRepository.findByPartyIdAndPrimaryDocumentTrue(partyId)
                .or(() -> identificationDocumentJpaRepository.findFirstByPartyId(partyId));
        var kycProfile = kycProfileJpaRepository.findByCustomerId(customerId);

        if (party.isEmpty() || naturalPerson.isEmpty() || document.isEmpty() || kycProfile.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new NaturalPersonCustomer(
                mapper.toDomain(party.get()),
                mapper.toDomain(naturalPerson.get()),
                mapper.toDomain(customerEntity),
                mapper.toDomain(document.get()),
                contactPointJpaRepository.findByPartyId(partyId).stream().map(mapper::toDomain).toList(),
                addressJpaRepository.findByPartyId(partyId).stream().map(mapper::toDomain).toList(),
                mapper.toDomain(kycProfile.get()),
                customerStatusHistoryJpaRepository.findByCustomerIdOrderByChangedAtAsc(customerId).stream().map(mapper::toDomain).toList()
        ));
    }
}
