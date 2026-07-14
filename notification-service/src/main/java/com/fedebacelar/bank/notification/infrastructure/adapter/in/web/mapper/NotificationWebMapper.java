package com.fedebacelar.bank.notification.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto.NotificationResponse;
import com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto.SendEmailNotificationRequest;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationWebMapper {

    public SendEmailNotificationCommand toCommand(SendEmailNotificationRequest request) {
        return new SendEmailNotificationCommand(
                request.recipient(),
                request.templateCode(),
                request.variables() == null ? Map.of() : request.variables(),
                request.correlationId(),
                Boolean.TRUE.equals(request.sensitive())
        );
    }

    public NotificationResponse toResponse(NotificationDetails details) {
        return new NotificationResponse(
                details.id(),
                details.channel(),
                details.recipient(),
                details.templateCode(),
                details.correlationId(),
                details.subject(),
                details.status(),
                details.attemptCount(),
                details.lastError(),
                details.sentAt(),
                details.createdAt(),
                details.updatedAt()
        );
    }
}

