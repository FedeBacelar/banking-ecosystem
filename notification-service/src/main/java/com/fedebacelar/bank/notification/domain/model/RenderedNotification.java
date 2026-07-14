package com.fedebacelar.bank.notification.domain.model;

public record RenderedNotification(
        String subject,
        String textBody,
        String htmlBody
) {
}

