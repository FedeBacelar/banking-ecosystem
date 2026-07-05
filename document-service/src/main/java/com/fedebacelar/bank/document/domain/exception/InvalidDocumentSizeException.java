package com.fedebacelar.bank.document.domain.exception;

public class InvalidDocumentSizeException extends RuntimeException {

    public InvalidDocumentSizeException(long size, long maxSize) {
        super("Invalid document size " + size + " bytes. Maximum allowed size is " + maxSize + " bytes.");
    }
}

