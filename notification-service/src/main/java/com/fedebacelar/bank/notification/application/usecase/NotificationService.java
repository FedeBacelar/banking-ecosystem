package com.fedebacelar.bank.notification.application.usecase;

import com.fedebacelar.bank.notification.application.command.SendEmailNotificationCommand;
import com.fedebacelar.bank.notification.application.mapper.NotificationDetailsMapper;
import com.fedebacelar.bank.notification.application.port.in.SendEmailNotificationUseCase;
import com.fedebacelar.bank.notification.application.port.out.EmailDeliveryPort;
import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.application.port.out.TemplateRendererPort;
import com.fedebacelar.bank.notification.application.view.NotificationDetails;
import com.fedebacelar.bank.notification.domain.exception.EmailDeliveryException;
import com.fedebacelar.bank.notification.domain.model.Notification;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService implements SendEmailNotificationUseCase {

    private final NotificationRepositoryPort repositoryPort;
    private final TemplateRendererPort templateRendererPort;
    private final EmailDeliveryPort emailDeliveryPort;
    private final Clock clock;

    public NotificationService(
            NotificationRepositoryPort repositoryPort,
            TemplateRendererPort templateRendererPort,
            EmailDeliveryPort emailDeliveryPort,
            Clock clock
    ) {
        this.repositoryPort = repositoryPort;
        this.templateRendererPort = templateRendererPort;
        this.emailDeliveryPort = emailDeliveryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public NotificationDetails send(SendEmailNotificationCommand command) {
        Instant now = Instant.now(clock);
        Notification notification = Notification.createEmail(
                command.recipient(),
                command.templateCode(),
                command.variables() == null ? java.util.Map.of() : command.variables(),
                command.correlationId(),
                templateRendererPort.render(command.templateCode(), command.variables() == null ? java.util.Map.of() : command.variables()),
                now
        );

        notification = repositoryPort.save(notification);

        try {
            emailDeliveryPort.deliver(notification);
            notification = repositoryPort.save(notification.markSent(Instant.now(clock)));
        } catch (EmailDeliveryException exception) {
            notification = repositoryPort.save(notification.markFailed(exception.getMessage(), Instant.now(clock)));
        }

        return NotificationDetailsMapper.toDetails(notification);
    }
}

