package com.fedebacelar.bank.document.domain.exception;

public class InvalidDocumentContentTypeException extends RuntimeException {

    public InvalidDocumentContentTypeException(String contentType) {
        super("Unsupported document content type: " + contentType);
    }
}

