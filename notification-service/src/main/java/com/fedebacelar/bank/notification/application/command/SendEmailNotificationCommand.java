package com.fedebacelar.bank.notification.application.command;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.util.Map;

public record SendEmailNotificationCommand(
        String recipient,
        NotificationTemplateCode templateCode,
        Map<String, String> variables,
        String correlationId
) {
}

