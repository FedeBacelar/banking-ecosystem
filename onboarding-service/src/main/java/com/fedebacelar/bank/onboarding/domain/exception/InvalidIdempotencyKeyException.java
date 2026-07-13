package com.fedebacelar.bank.onboarding.domain.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must contain between 1 and 128 characters.");
    }
}
