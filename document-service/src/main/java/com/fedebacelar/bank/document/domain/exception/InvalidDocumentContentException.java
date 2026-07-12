package com.fedebacelar.bank.document.domain.exception;

public class InvalidDocumentContentException extends RuntimeException {
    public InvalidDocumentContentException() {
        super("The uploaded file content does not match its declared document type.");
    }

    public InvalidDocumentContentException(Throwable cause) {
        super("The uploaded file content could not be inspected.", cause);
    }
}
