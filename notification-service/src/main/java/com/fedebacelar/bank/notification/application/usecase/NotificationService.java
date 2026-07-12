package com.fedebacelar.bank.notification.application.usecase;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.mapper.NotificationDetailsMapper;
import com.fedebacelar.bank.notification.application.port.in.SendEmailNotificationUseCase;
import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.domain.enums.NotificationStatus;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.domain.model.RenderedNotification;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements SendEmailNotificationUseCase {

    private final NotificationRepositoryPort repository;
    private final TemplateRendererPort templateRenderer;
    private final EmailDeliveryPort emailDelivery;
    private final Clock clock;

    public NotificationService(
            NotificationRepositoryPort repository,
            TemplateRendererPort templateRenderer,
            EmailDeliveryPort emailDelivery,
            Clock clock
    ) {
        this.repository = repository;
        this.templateRenderer = templateRenderer;
        this.emailDelivery = emailDelivery;
        this.clock = clock;
    }

    @Override
    public NotificationDetails send(SendEmailNotificationCommand command) {
        Notification existing = existing(command);
        if (existing != null && existing.status() == NotificationStatus.SENT) {
            return NotificationDetailsMapper.toDetails(existing);
        }

        Instant now = Instant.now(clock);
        Map<String, String> variables = command.variables() == null ? Map.of() : command.variables();
        RenderedNotification rendered = templateRenderer.render(command.templateCode(), variables);
        Notification delivery = existing == null
                ? Notification.createEmail(
                        command.recipient(), command.templateCode(), variables,
                        command.correlationId(), rendered, now
                )
                : existing.prepareDelivery(variables, rendered, now);

        Notification auditRecord = command.sensitive() ? delivery.redactContent() : delivery;
        auditRecord = repository.save(auditRecord);

        try {
            emailDelivery.deliver(delivery);
            auditRecord = repository.save(auditRecord.markSent(Instant.now(clock)));
        } catch (EmailDeliveryException exception) {
            auditRecord = repository.save(auditRecord.markFailed(
                    sanitizedError(exception), Instant.now(clock)
            ));
        }

        return NotificationDetailsMapper.toDetails(auditRecord);
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
