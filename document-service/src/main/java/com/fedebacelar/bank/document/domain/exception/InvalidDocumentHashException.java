package com.fedebacelar.bank.document.domain.exception;

public class InvalidDocumentHashException extends RuntimeException {
    public InvalidDocumentHashException() {
        super("The uploaded document content does not match its declared digest.");
    }
}
