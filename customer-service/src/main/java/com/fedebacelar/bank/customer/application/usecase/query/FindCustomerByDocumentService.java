package com.fedebacelar.bank.customer.application.usecase.query;

import com.fedebacelar.bank.customer.application.mapper.CustomerDetailsMapper;
import com.fedebacelar.bank.customer.application.port.in.FindCustomerByDocumentUseCase;
import com.fedebacelar.bank.customer.application.port.out.CustomerRepositoryPort;
import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;
import com.fedebacelar.bank.customer.domain.exception.CustomerDocumentNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FindCustomerByDocumentService implements FindCustomerByDocumentUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    public FindCustomerByDocumentService(CustomerRepositoryPort customerRepositoryPort) {
        this.customerRepositoryPort = customerRepositoryPort;
    }

    @Override
    public CustomerDetails findByDocument(DocumentType type, String number, String country) {
        return customerRepositoryPort.findByDocument(type, number, country)
                .map(CustomerDetailsMapper::toDetails)
                .orElseThrow(() -> new CustomerDocumentNotFoundException(type, number, country));
    }
}
