package com.fedebacelar.bank.document.domain.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(UUID documentId) {
        super("Document was not found: " + documentId);
    }
}

