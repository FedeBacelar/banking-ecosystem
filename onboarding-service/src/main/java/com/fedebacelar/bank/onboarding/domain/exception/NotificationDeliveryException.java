package com.fedebacelar.bank.onboarding.domain.exception;

import java.util.UUID;

public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(UUID applicationId, Throwable cause) {
        super("Could not send onboarding notification for application: " + applicationId, cause);
    }
}
