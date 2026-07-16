package com.fedebacelar.bank.notification.application.usecase;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.mapper.NotificationDetailsMapper;
import com.fedebacelar.bank.notification.application.port.in.SendEmailNotificationUseCase;
import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort;
import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.exception.NotificationRequestConflictException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

import static com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort.DeliveryOutcome.FAILED;
import static com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort.DeliveryOutcome.REPLAYED;
import static com.fedebacelar.bank.notification.application.port.out.NotificationTelemetryPort.DeliveryOutcome.SENT;

@Service
public class NotificationService implements SendEmailNotificationUseCase {

    private final NotificationRepositoryPort repository;
    private final TemplateRendererPort templateRenderer;
    private final EmailDeliveryPort emailDelivery;
    private final NotificationTelemetryPort telemetry;
    private final Clock clock;

    public NotificationService(
            NotificationRepositoryPort repository,
            TemplateRendererPort templateRenderer,
            EmailDeliveryPort emailDelivery,
            NotificationTelemetryPort telemetry,
            Clock clock
    ) {
        this.repository = repository;
        this.templateRenderer = templateRenderer;
        this.emailDelivery = emailDelivery;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Override
    public NotificationDetails send(SendEmailNotificationCommand command) {
        Map<String, String> variables = command.variables() == null ? Map.of() : command.variables();
        RenderedNotification rendered = templateRenderer.render(command.templateCode(), variables);
        String requestFingerprint = NotificationRequestFingerprint.calculate(
                command.recipient(), command.templateCode(), variables
        );
        boolean redactContent = command.templateCode().requiresRedaction() || command.sensitive();
        Notification existing = existing(command);
        validateReplay(existing, command.recipient(), requestFingerprint);

        if (existing != null && existing.status() == NotificationStatus.SENT) {
            Notification auditRecord = redactIfRequired(existing, redactContent);
            if (auditRecord != existing) {
                auditRecord = repository.save(auditRecord);
            }
            telemetry.recordDelivery(command.templateCode(), REPLAYED);
            return NotificationDetailsMapper.toDetails(auditRecord);
        }

        Instant now = Instant.now(clock);
        Notification delivery = existing == null
                ? Notification.createEmail(
                        command.recipient(), command.templateCode(), variables,
                        command.correlationId(), requestFingerprint, rendered, now
                )
                : existing.prepareDelivery(variables, rendered, requestFingerprint, now);

        Notification auditRecord = redactIfRequired(delivery, redactContent);
        auditRecord = repository.save(auditRecord);

        try {
            emailDelivery.deliver(delivery);
            auditRecord = repository.save(auditRecord.markSent(Instant.now(clock)));
            telemetry.recordDelivery(command.templateCode(), SENT);
        } catch (EmailDeliveryException exception) {
            auditRecord = repository.save(auditRecord.markFailed(
                    sanitizedError(exception), Instant.now(clock)
            ));
            telemetry.recordDelivery(command.templateCode(), FAILED);
        }

        return NotificationDetailsMapper.toDetails(auditRecord);
    }

    private void validateReplay(
            Notification existing,
            String recipient,
            String requestFingerprint
    ) {
        if (existing == null) {
            return;
        }
        if (!Objects.equals(existing.recipient(), recipient)) {
            throw new NotificationRequestConflictException();
        }
        if (existing.requestFingerprint() != null
                && !existing.requestFingerprint().equals(requestFingerprint)) {
            throw new NotificationRequestConflictException();
        }
    }

    private Notification redactIfRequired(Notification notification, boolean required) {
        return required && !notification.contentRedacted()
                ? notification.redactContent()
                : notification;
    }

    private Notification existing(SendEmailNotificationCommand command) {
        if (command.correlationId() == null || command.correlationId().isBlank()) {
            return null;
        }
        return repository.findByTemplateCodeAndCorrelationId(
                command.templateCode(), command.correlationId()
        ).orElse(null);
    }

    private String sanitizedError(EmailDeliveryException exception) {
        return exception.getClass().getSimpleName();
    }
}
