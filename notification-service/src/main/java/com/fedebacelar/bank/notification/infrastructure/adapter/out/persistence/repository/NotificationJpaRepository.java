package com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.notification.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
}
