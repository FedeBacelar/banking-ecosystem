package com.fedebacelar.bank.notification.application.port.out;

import com.fedebacelar.bank.notification.domain.model.Notification;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID notificationId);
}

