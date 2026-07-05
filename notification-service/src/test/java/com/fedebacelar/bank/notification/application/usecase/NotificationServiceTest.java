package com.fedebacelar.bank.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final NotificationRepositoryPort repositoryPort = mock(NotificationRepositoryPort.class);
    private final TemplateRendererPort templateRendererPort = mock(TemplateRendererPort.class);
    private final EmailDeliveryPort emailDeliveryPort = mock(EmailDeliveryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-04T21:00:00Z"), ZoneOffset.UTC);
    private final NotificationService service = new NotificationService(repositoryPort, templateRendererPort, emailDeliveryPort, clock);

    @Test
    void sendsEmailNotification() {
        when(templateRendererPort.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of("magicLink", "http://localhost", "expiresInMinutes", "30")))
                .thenReturn(new RenderedNotification("Subject", "Body"));
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.send(new SendEmailNotificationCommand(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", "http://localhost", "expiresInMinutes", "30"),
                "application-1"
        ));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(details.attemptCount()).isEqualTo(1);
        assertThat(details.sentAt()).isEqualTo(Instant.parse("2026-07-04T21:00:00Z"));
    }

    @Test
    void marksNotificationAsFailedWhenDeliveryFails() {
        when(templateRendererPort.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, Map.of("magicLink", "http://localhost", "expiresInMinutes", "30")))
                .thenReturn(new RenderedNotification("Subject", "Body"));
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new EmailDeliveryException("Email delivery failed: smtp unavailable", new RuntimeException("smtp unavailable")))
                .when(emailDeliveryPort).deliver(any(Notification.class));

        var details = service.send(new SendEmailNotificationCommand(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                Map.of("magicLink", "http://localhost", "expiresInMinutes", "30"),
                "application-1"
        ));

        assertThat(details.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(details.attemptCount()).isEqualTo(1);
        assertThat(details.lastError()).contains("smtp unavailable");
    }
}

