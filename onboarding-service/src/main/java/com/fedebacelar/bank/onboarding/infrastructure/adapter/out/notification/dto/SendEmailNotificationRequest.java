package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.notification.dto;

import java.util.Map;

public record SendEmailNotificationRequest(
        String recipient,
        String templateCode,
        Map<String, String> variables,
        String correlationId
) {
}
