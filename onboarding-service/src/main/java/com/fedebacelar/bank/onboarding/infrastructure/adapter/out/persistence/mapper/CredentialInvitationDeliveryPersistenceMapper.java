package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.CredentialInvitationDeliveryEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CredentialInvitationDeliveryPersistenceMapper {
    public CredentialInvitationDeliveryEntity toEntity(CredentialInvitationDelivery delivery) {
        CredentialInvitationDeliveryEntity entity = new CredentialInvitationDeliveryEntity();
        entity.setId(delivery.id().toString());
        entity.setApplicationId(delivery.applicationId().toString());
        entity.setIdempotencyKeyHash(delivery.idempotencyKeyHash());
        entity.setStatus(delivery.status());
        entity.setAttempts(delivery.attempts());
        entity.setNextAttemptAt(delivery.nextAttemptAt());
        entity.setLockedUntil(delivery.lockedUntil());
        entity.setLastErrorCode(delivery.lastErrorCode());
        entity.setSentAt(delivery.sentAt());
        entity.setCreatedAt(delivery.createdAt());
        entity.setUpdatedAt(delivery.updatedAt());
        entity.setVersion(delivery.version());
        return entity;
    }

    public CredentialInvitationDelivery toDomain(CredentialInvitationDeliveryEntity entity) {
        return new CredentialInvitationDelivery(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getApplicationId()),
                entity.getIdempotencyKeyHash(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getNextAttemptAt(),
                entity.getLockedUntil(),
                entity.getLastErrorCode(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
