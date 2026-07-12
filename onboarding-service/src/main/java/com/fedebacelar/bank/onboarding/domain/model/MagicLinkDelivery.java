package com.fedebacelar.bank.onboarding.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MagicLinkDelivery(
        UUID applicationId,
        UUID deliveryId,
        String recipient,
        String encryptedMagicLink,
        Instant expiresAt,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static MagicLinkDelivery pending(
            UUID applicationId,
            String recipient,
            String encryptedMagicLink,
            Instant expiresAt,
            Instant now
    ) {
        return new MagicLinkDelivery(
                applicationId, UUID.randomUUID(), recipient, encryptedMagicLink,
                expiresAt, null, now, now, 0L
        );
    }

    public MagicLinkDelivery replace(
            String newRecipient,
            String newEncryptedMagicLink,
            Instant newExpiresAt,
            Instant now
    ) {
        return new MagicLinkDelivery(
                applicationId, UUID.randomUUID(), newRecipient, newEncryptedMagicLink,
                newExpiresAt, null, createdAt, now, version
        );
    }

    public MagicLinkDelivery markSent(Instant now) {
        return new MagicLinkDelivery(
                applicationId, deliveryId, recipient, null, expiresAt,
                now, createdAt, now, version
        );
    }

    public MagicLinkDelivery discardPayload(Instant now) {
        return new MagicLinkDelivery(
                applicationId, deliveryId, recipient, null, expiresAt,
                sentAt, createdAt, now, version
        );
    }

    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
