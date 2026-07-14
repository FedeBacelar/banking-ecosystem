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
        String requestFingerprint,
        String subject,
        String body,
        String htmlBody,
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
            String requestFingerprint,
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
                requestFingerprint,
                renderedNotification.subject(),
                renderedNotification.textBody(),
                renderedNotification.htmlBody(),
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
                requestFingerprint,
                subject,
                body,
                htmlBody,
                NotificationStatus.SENT,
                attemptCount + 1,
                null,
                now,
                createdAt,
                now,
                version
        );
    }

    public Notification prepareDelivery(
            Map<String, String> newVariables,
            RenderedNotification renderedNotification,
            String newRequestFingerprint,
            Instant now
    ) {
        return new Notification(
                id,
                channel,
                recipient,
                templateCode,
                Map.copyOf(newVariables),
                correlationId,
                requestFingerprint == null ? newRequestFingerprint : requestFingerprint,
                renderedNotification.subject(),
                renderedNotification.textBody(),
                renderedNotification.htmlBody(),
                NotificationStatus.PENDING,
                attemptCount,
                null,
                sentAt,
                createdAt,
                now,
                version
        );
    }

    public Notification redactContent() {
        return new Notification(
                id,
                channel,
                recipient,
                templateCode,
                Map.of(),
                correlationId,
                requestFingerprint,
                subject,
                "[REDACTED]",
                "[REDACTED]",
                status,
                attemptCount,
                lastError,
                sentAt,
                createdAt,
                updatedAt,
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
                requestFingerprint,
                subject,
                body,
                htmlBody,
                NotificationStatus.FAILED,
                attemptCount + 1,
                error,
                sentAt,
                createdAt,
                now,
                version
        );
    }

    public boolean contentRedacted() {
        return variables.isEmpty()
                && "[REDACTED]".equals(body)
                && "[REDACTED]".equals(htmlBody);
    }
}

