package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import com.fedebacelar.bank.notification.domain.enums.NotificationTemplateCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
    Optional<NotificationEntity> findByTemplateCodeAndCorrelationId(
            NotificationTemplateCode templateCode,
            String correlationId
    );
}
