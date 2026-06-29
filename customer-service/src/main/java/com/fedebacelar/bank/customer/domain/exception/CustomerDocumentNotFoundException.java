package com.fedebacelar.bank.customer.domain.exception;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;

public class CustomerDocumentNotFoundException extends RuntimeException {

    public CustomerDocumentNotFoundException(DocumentType type, String number, String country) {
        super("Customer not found for document: " + type + " " + number + " " + country);
    }
}
