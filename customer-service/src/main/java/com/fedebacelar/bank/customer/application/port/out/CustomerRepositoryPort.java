package com.fedebacelar.bank.customer.application.port.out;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.model.CustomerStatusHistory;
import com.fedebacelar.bank.customer.domain.model.NaturalPersonCustomer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort {

    NaturalPersonCustomer save(NaturalPersonCustomer customer);

    Optional<NaturalPersonCustomer> findByCustomerId(UUID customerId);

    Optional<NaturalPersonCustomer> findByDocument(DocumentType type, String number, String country);

    Optional<NaturalPersonCustomer> findByEmail(String email);

    Optional<NaturalPersonCustomer> findByCustomerNumber(String customerNumber);

    List<CustomerStatusHistory> findStatusHistory(UUID customerId);

    boolean existsDocument(DocumentType type, String number, String country);

}
