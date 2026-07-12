package com.fedebacelar.bank.document.domain.exception;

public class DocumentIdempotencyConflictException extends RuntimeException {
    public DocumentIdempotencyConflictException() {
        super("The idempotency key was already used for a different document.");
    }
}
