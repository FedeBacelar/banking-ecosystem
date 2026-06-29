package com.fedebacelar.bank.customer.application.port.out;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;

public interface IdentificationDocumentLookupPort {

    boolean existsDocument(DocumentType type, String number, String country);

}
