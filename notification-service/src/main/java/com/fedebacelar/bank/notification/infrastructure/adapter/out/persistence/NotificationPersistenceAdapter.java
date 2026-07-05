package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.notification.application.port.out.NotificationRepositoryPort;
import com.fedebacelar.bank.notification.domain.model.Notification;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.mapper.NotificationPersistenceMapper;
import com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.repository.NotificationJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository repository;
    private final NotificationPersistenceMapper mapper;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository, NotificationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Notification save(Notification notification) {
        return mapper.toDomain(repository.save(mapper.toEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(UUID notificationId) {
        return repository.findById(notificationId.toString()).map(mapper::toDomain);
    }
}
