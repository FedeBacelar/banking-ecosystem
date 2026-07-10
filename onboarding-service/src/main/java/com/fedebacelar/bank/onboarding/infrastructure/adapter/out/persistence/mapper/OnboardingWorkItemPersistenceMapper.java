package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingWorkItemEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingWorkItemPersistenceMapper {
    public OnboardingWorkItemEntity toEntity(OnboardingWorkItem item) {
        OnboardingWorkItemEntity entity = new OnboardingWorkItemEntity();
        entity.setId(item.id().toString());
        entity.setApplicationId(item.applicationId().toString());
        entity.setJobType(item.jobType());
        entity.setStatus(item.status());
        entity.setAttempts(item.attempts());
        entity.setNextAttemptAt(item.nextAttemptAt());
        entity.setLockedUntil(item.lockedUntil());
        entity.setLastErrorCode(item.lastErrorCode());
        entity.setCreatedAt(item.createdAt());
        entity.setUpdatedAt(item.updatedAt());
        entity.setVersion(item.version());
        return entity;
    }

    public OnboardingWorkItem toDomain(OnboardingWorkItemEntity entity) {
        return new OnboardingWorkItem(UUID.fromString(entity.getId()), UUID.fromString(entity.getApplicationId()),
                entity.getJobType(), entity.getStatus(), entity.getAttempts(), entity.getNextAttemptAt(),
                entity.getLockedUntil(), entity.getLastErrorCode(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
