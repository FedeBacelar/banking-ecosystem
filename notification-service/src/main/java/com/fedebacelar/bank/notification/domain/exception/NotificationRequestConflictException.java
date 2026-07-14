package com.fedebacelar.bank.notification.domain.exception;

public class NotificationRequestConflictException extends RuntimeException {

    public NotificationRequestConflictException() {
        super("Notification idempotency key is already associated with a different request");
    }
}
