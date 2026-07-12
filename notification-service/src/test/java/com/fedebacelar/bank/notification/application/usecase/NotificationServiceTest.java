package com.fedebacelar.bank.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-04T21:00:00Z");
    private static final Map<String, String> VARIABLES = Map.of(
            "magicLink", "http://localhost/onboarding/continue#token=secret",
            "expiresInMinutes", "30"
    );

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final TemplateRendererPort renderer = mock(TemplateRendererPort.class);
    private final EmailDeliveryPort emailDelivery = mock(EmailDeliveryPort.class);
    private final NotificationService service = new NotificationService(
            repository,
            renderer,
            emailDelivery,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldDeliverSensitiveContentInMemoryButPersistOnlyRedactedAuditData() {
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(new RenderedNotification("Subject", "Body with secret link"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.send(command(true));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        verify(emailDelivery).deliver(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.body().contains("secret link")
                        && notification.variables().containsKey("magicLink")
        ));
        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(persisted.capture());
        assertThat(persisted.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.body()).isEqualTo("[REDACTED]");
            assertThat(notification.variables()).isEmpty();
        });
    }

    @Test
    void shouldPersistOnlySanitizedFailureInformation() {
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(new RenderedNotification("Subject", "Body with secret link"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new EmailDeliveryException(
                "Email delivery failed: smtp.internal.example password=secret",
                new RuntimeException("smtp unavailable")
        )).when(emailDelivery).deliver(any(Notification.class));

        var details = service.send(command(true));

        assertThat(details.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(details.lastError()).isEqualTo("EmailDeliveryException");
        assertThat(details.lastError()).doesNotContain("smtp", "password", "secret");
        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(persisted.capture());
        assertThat(persisted.getAllValues().getLast().body()).isEqualTo("[REDACTED]");
        assertThat(persisted.getAllValues().getLast().variables()).isEmpty();
    }

    @Test
    void shouldReturnAnExistingSentNotificationWithoutSendingAgain() {
        Notification sent = Notification.createEmail(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                VARIABLES,
                "delivery-1",
                new RenderedNotification("Subject", "Body with secret link"),
                NOW.minusSeconds(10)
        ).redactContent().markSent(NOW.minusSeconds(5));
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(sent));

        var details = service.send(command(true));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(details.id()).isEqualTo(sent.id());
        verifyNoInteractions(renderer, emailDelivery);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    private SendEmailNotificationCommand command(boolean sensitive) {
        return new SendEmailNotificationCommand(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                VARIABLES,
                "delivery-1",
                sensitive
        );
    }
}
