package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingStatusHistoryEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingStatusHistoryPersistenceMapper {
    public OnboardingStatusHistoryEntity toEntity(OnboardingStatusHistory history) {
        OnboardingStatusHistoryEntity entity = new OnboardingStatusHistoryEntity();
        entity.setId(history.id().toString());
        entity.setApplicationId(history.applicationId().toString());
        entity.setPreviousStatus(history.previousStatus());
        entity.setNewStatus(history.newStatus());
        entity.setReasonCode(history.reasonCode());
        entity.setActorType(history.actorType());
        entity.setOccurredAt(history.occurredAt());
        return entity;
    }

    public OnboardingStatusHistory toDomain(OnboardingStatusHistoryEntity entity) {
        return new OnboardingStatusHistory(UUID.fromString(entity.getId()), UUID.fromString(entity.getApplicationId()),
                entity.getPreviousStatus(), entity.getNewStatus(), entity.getReasonCode(), entity.getActorType(), entity.getOccurredAt());
    }
}
