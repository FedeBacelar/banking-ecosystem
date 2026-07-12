package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.MagicLinkDeliveryEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MagicLinkDeliveryPersistenceMapper {
    public MagicLinkDeliveryEntity toEntity(MagicLinkDelivery delivery) {
        MagicLinkDeliveryEntity entity = new MagicLinkDeliveryEntity();
        entity.setApplicationId(delivery.applicationId().toString());
        entity.setDeliveryId(delivery.deliveryId().toString());
        entity.setRecipient(delivery.recipient());
        entity.setEncryptedMagicLink(delivery.encryptedMagicLink());
        entity.setExpiresAt(delivery.expiresAt());
        entity.setSentAt(delivery.sentAt());
        entity.setCreatedAt(delivery.createdAt());
        entity.setUpdatedAt(delivery.updatedAt());
        entity.setVersion(delivery.version());
        return entity;
    }

    public MagicLinkDelivery toDomain(MagicLinkDeliveryEntity entity) {
        return new MagicLinkDelivery(
                UUID.fromString(entity.getApplicationId()),
                UUID.fromString(entity.getDeliveryId()),
                entity.getRecipient(),
                entity.getEncryptedMagicLink(),
                entity.getExpiresAt(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
