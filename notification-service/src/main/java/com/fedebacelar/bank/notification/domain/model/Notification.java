package com.fedebacelar.bank.notification.domain.model;

import com.fedebacelar.bank.notification.domain.enums.NotificationChannel;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Notification(
        UUID id,
        NotificationChannel channel,
        String recipient,
        NotificationTemplateCode templateCode,
        Map<String, String> variables,
        String correlationId,
        String subject,
        String body,
        NotificationStatus status,
        int attemptCount,
        String lastError,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public static Notification createEmail(
            String recipient,
            NotificationTemplateCode templateCode,
            Map<String, String> variables,
            String correlationId,
            RenderedNotification renderedNotification,
            Instant now
    ) {
        return new Notification(
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                recipient,
                templateCode,
                Map.copyOf(variables),
                correlationId,
                renderedNotification.subject(),
                renderedNotification.body(),
                NotificationStatus.PENDING,
                0,
                null,
                null,
                now,
                now,
                0L
        );
    }

    public Notification markSent(Instant now) {
        return new Notification(
                id,
                channel,
                recipient,
                templateCode,
                variables,
                correlationId,
                subject,
                body,
                NotificationStatus.SENT,
                attemptCount + 1,
                null,
                now,
                createdAt,
                now,
                version
        );
    }

    public Notification markFailed(String error, Instant now) {
        return new Notification(
                id,
                channel,
                recipient,
                templateCode,
                variables,
                correlationId,
                subject,
                body,
                NotificationStatus.FAILED,
                attemptCount + 1,
                error,
                sentAt,
                createdAt,
                now,
                version
        );
    }
}

