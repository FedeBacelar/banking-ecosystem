package com.fedebacelar.bank.notification.infrastructure.adapter.in.web.dto;

import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SendEmailNotificationRequest(
        @NotBlank @Email @Size(max = 255) String recipient,
        @NotNull NotificationTemplateCode templateCode,
        Map<@NotBlank @Size(max = 80) String, @NotBlank @Size(max = 2000) String> variables,
        @Size(max = 120) String correlationId,
        Boolean sensitive
) {
}

