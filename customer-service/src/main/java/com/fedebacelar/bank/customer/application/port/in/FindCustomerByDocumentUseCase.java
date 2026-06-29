package com.fedebacelar.bank.customer.application.port.in;

import com.fedebacelar.bank.customer.application.view.CustomerDetails;
import com.fedebacelar.bank.customer.domain.enums.DocumentType;

public interface FindCustomerByDocumentUseCase {

    CustomerDetails findByDocument(DocumentType type, String number, String country);

}
