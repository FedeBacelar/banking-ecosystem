package com.fedebacelar.bank.customer.domain.exception;

import com.fedebacelar.bank.customer.domain.enums.DocumentType;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException(DocumentType type, String number, String country) {
        super("Document already registered: " + type + " " + number + " " + country);
    }
}
