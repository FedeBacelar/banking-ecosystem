package com.fedebacelar.bank.onboarding.application.port.out;

import java.time.Duration;
import java.util.UUID;

public interface NotificationPort {

    void sendMagicLink(
            UUID deliveryId,
            UUID applicationId,
            String recipient,
            String magicLink,
            Duration expiresIn
    );
}
