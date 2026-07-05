package com.fedebacelar.bank.notification.application.mapper;

import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.domain.model.Notification;

public final class NotificationDetailsMapper {

    private NotificationDetailsMapper() {
    }

    public static NotificationDetails toDetails(Notification notification) {
        return new NotificationDetails(
                notification.id(),
                notification.channel(),
                notification.recipient(),
                notification.templateCode(),
                notification.correlationId(),
                notification.subject(),
                notification.status(),
                notification.attemptCount(),
                notification.lastError(),
                notification.sentAt(),
                notification.createdAt(),
                notification.updatedAt()
        );
    }
}

