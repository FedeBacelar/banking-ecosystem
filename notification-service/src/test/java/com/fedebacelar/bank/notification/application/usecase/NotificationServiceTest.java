package com.fedebacelar.bank.notification.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.NotificationRequestConflictException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.template.EmailActionLinkPolicy;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.template.InMemoryTemplateRendererAdapter;
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
            "magicLink", "http://localhost:4200/onboarding/continue#token=" + "A".repeat(43),
            "expiresInMinutes", "30"
    );
    private static final RenderedNotification RENDERED = new RenderedNotification(
            "Subject",
            "Text body with sensitive link",
            "<html>HTML body with sensitive link</html>"
    );

    private final NotificationRepositoryPort repository = mock(NotificationRepositoryPort.class);
    private final TemplateRendererPort renderer = mock(TemplateRendererPort.class);
    private final EmailDeliveryPort emailDelivery = mock(EmailDeliveryPort.class);
    private final NotificationTelemetryPort telemetry = mock(NotificationTelemetryPort.class);
    private final NotificationService service = new NotificationService(
            repository,
            renderer,
            emailDelivery,
            telemetry,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void shouldDeliverSensitiveContentInMemoryButPersistOnlyRedactedAuditData() {
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.send(command(true));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        verify(emailDelivery).deliver(org.mockito.ArgumentMatchers.argThat(notification ->
                notification.body().contains("sensitive link")
                        && notification.htmlBody().contains("sensitive link")
                        && notification.variables().containsKey("magicLink")
                        && fingerprint("person@example.com", VARIABLES)
                        .equals(notification.requestFingerprint())
        ));
        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(persisted.capture());
        assertThat(persisted.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.contentRedacted()).isTrue();
            assertThat(notification.requestFingerprint())
                    .isEqualTo(fingerprint("person@example.com", VARIABLES));
        });
        verify(telemetry).recordDelivery(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                NotificationTelemetryPort.DeliveryOutcome.SENT
        );
    }

    @Test
    void shouldNotAllowTheCallerToDisableTemplateRequiredRedaction() {
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.send(command(false));

        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(persisted.capture());
        assertThat(persisted.getAllValues()).allSatisfy(notification ->
                assertThat(notification.contentRedacted()).isTrue()
        );
    }

    @Test
    void shouldPersistOnlySanitizedFailureInformation() {
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);
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
        assertThat(persisted.getAllValues().getLast().contentRedacted()).isTrue();
        verify(telemetry).recordDelivery(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                NotificationTelemetryPort.DeliveryOutcome.FAILED
        );
    }

    @Test
    void shouldValidateButNotResendAnIdenticalSentReplay() {
        Notification sent = sentNotification(
                "person@example.com",
                VARIABLES,
                fingerprint("person@example.com", VARIABLES),
                true
        );
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(sent));
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);

        var details = service.send(command(true));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(details.id()).isEqualTo(sent.id());
        verify(renderer).render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES);
        verifyNoInteractions(emailDelivery);
        verify(repository, org.mockito.Mockito.never()).save(any());
        verify(telemetry).recordDelivery(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                NotificationTelemetryPort.DeliveryOutcome.REPLAYED
        );
    }

    @Test
    void shouldRejectUnsafeLinkAndAdditionalVariablesBeforeSentReplayFastPath() {
        Notification sent = sentNotification(
                "person@example.com",
                VARIABLES,
                fingerprint("person@example.com", VARIABLES),
                true
        );
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(sent));
        NotificationService hardenedService = serviceWithRealRenderer();

        Map<String, String> unsafeVariables = Map.of(
                "magicLink", "https://attacker.example/onboarding/continue#token=" + "A".repeat(43),
                "expiresInMinutes", "30"
        );
        assertThatThrownBy(() -> hardenedService.send(command(
                "person@example.com", unsafeVariables, false
        )))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageNotContaining("attacker.example");

        Map<String, String> additionalVariables = Map.of(
                "magicLink", VARIABLES.get("magicLink"),
                "expiresInMinutes", "30",
                "hiddenSecret", "must-not-be-reflected"
        );
        assertThatThrownBy(() -> hardenedService.send(command(
                "person@example.com", additionalVariables, false
                )))
                .isInstanceOf(InvalidTemplateVariableException.class)
                .hasMessageNotContaining("hiddenSecret")
                .hasMessageNotContaining("must-not-be-reflected");

        verifyNoInteractions(repository, emailDelivery);
    }

    @Test
    void shouldRejectRecipientOrFingerprintMismatchForAnExistingIdempotencyKey() {
        Notification sent = sentNotification(
                "person@example.com",
                VARIABLES,
                fingerprint("person@example.com", VARIABLES),
                true
        );
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(sent));
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);

        assertThatThrownBy(() -> service.send(command(
                "other@example.com", VARIABLES, false
        )))
                .isInstanceOf(NotificationRequestConflictException.class)
                .hasMessageNotContaining("person@example.com")
                .hasMessageNotContaining("other@example.com");

        Map<String, String> changedVariables = Map.of(
                "magicLink", VARIABLES.get("magicLink"),
                "expiresInMinutes", "31"
        );
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, changedVariables))
                .thenReturn(RENDERED);
        assertThatThrownBy(() -> service.send(command(
                "person@example.com", changedVariables, false
        )))
                .isInstanceOf(NotificationRequestConflictException.class);

        verifyNoInteractions(emailDelivery);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldSanitizeLegacySentReplayWithoutAssigningAFingerprintOrResending() {
        Notification legacySent = sentNotification(
                "person@example.com",
                VARIABLES,
                null,
                false
        );
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(legacySent));
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.send(command(false));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(persisted.capture());
        assertThat(persisted.getValue().contentRedacted()).isTrue();
        assertThat(persisted.getValue().requestFingerprint()).isNull();
        verifyNoInteractions(emailDelivery);
    }

    @Test
    void shouldAssignFingerprintWhenALegacyFailedNotificationIsActuallyRetried() {
        Notification legacyFailed = Notification.createEmail(
                "person@example.com",
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                VARIABLES,
                "delivery-1",
                null,
                RENDERED,
                NOW.minusSeconds(20)
        ).redactContent().markFailed("EmailDeliveryException", NOW.minusSeconds(10));
        when(repository.findByTemplateCodeAndCorrelationId(
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                "delivery-1"
        )).thenReturn(Optional.of(legacyFailed));
        when(renderer.render(NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK, VARIABLES))
                .thenReturn(RENDERED);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.send(command(false));

        assertThat(details.status()).isEqualTo(NotificationStatus.SENT);
        ArgumentCaptor<Notification> persisted = ArgumentCaptor.forClass(Notification.class);
        verify(repository, org.mockito.Mockito.times(2)).save(persisted.capture());
        assertThat(persisted.getAllValues()).allSatisfy(notification ->
                assertThat(notification.requestFingerprint())
                        .isEqualTo(fingerprint("person@example.com", VARIABLES))
        );
        verify(emailDelivery).deliver(org.mockito.ArgumentMatchers.argThat(notification ->
                fingerprint("person@example.com", VARIABLES)
                        .equals(notification.requestFingerprint())
        ));
    }

    private SendEmailNotificationCommand command(boolean sensitive) {
        return command("person@example.com", VARIABLES, sensitive);
    }

    private SendEmailNotificationCommand command(
            String recipient,
            Map<String, String> variables,
            boolean sensitive
    ) {
        return new SendEmailNotificationCommand(
                recipient,
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                variables,
                "delivery-1",
                sensitive
        );
    }

    private Notification sentNotification(
            String recipient,
            Map<String, String> variables,
            String requestFingerprint,
            boolean redacted
    ) {
        Notification notification = Notification.createEmail(
                recipient,
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                variables,
                "delivery-1",
                requestFingerprint,
                RENDERED,
                NOW.minusSeconds(10)
        );
        return (redacted ? notification.redactContent() : notification)
                .markSent(NOW.minusSeconds(5));
    }

    private String fingerprint(String recipient, Map<String, String> variables) {
        return NotificationRequestFingerprint.calculate(
                recipient,
                NotificationTemplateCode.ONBOARDING_EMAIL_MAGIC_LINK,
                variables
        );
    }

    private NotificationService serviceWithRealRenderer() {
        return new NotificationService(
                repository,
                new InMemoryTemplateRendererAdapter(new EmailActionLinkPolicy(
                        "http://localhost:4200",
                        "http://localhost:8090"
                )),
                emailDelivery,
                telemetry,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
