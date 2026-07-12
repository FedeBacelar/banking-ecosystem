package com.fedebacelar.bank.notification.domain.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID notificationId) {
        super("Notification was not found: " + notificationId);
    }
}

