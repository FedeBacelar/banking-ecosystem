package com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.notification.domain.enums.NotificationChannel;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationChannel channel,
        String recipient,
        NotificationTemplateCode templateCode,
        String correlationId,
        String subject,
        NotificationStatus status,
        int attemptCount,
        String lastError,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt
) {
}

