package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingProvisioningStepEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingProvisioningStepPersistenceMapper {
    public OnboardingProvisioningStepEntity toEntity(OnboardingProvisioningStep step) {
        OnboardingProvisioningStepEntity entity = new OnboardingProvisioningStepEntity();
        entity.setId(step.id().toString());
        entity.setApplicationId(step.applicationId().toString());
        entity.setStepType(step.stepType());
        entity.setStatus(step.status());
        entity.setRequestHash(step.requestHash());
        entity.setExternalReference(step.externalReference());
        entity.setAttempts(step.attempts());
        entity.setNextAttemptAt(step.nextAttemptAt());
        entity.setLastErrorCode(step.lastErrorCode());
        entity.setStartedAt(step.startedAt());
        entity.setCompletedAt(step.completedAt());
        entity.setCreatedAt(step.createdAt());
        entity.setUpdatedAt(step.updatedAt());
        entity.setVersion(step.version());
        return entity;
    }

    public OnboardingProvisioningStep toDomain(OnboardingProvisioningStepEntity entity) {
        return new OnboardingProvisioningStep(UUID.fromString(entity.getId()), UUID.fromString(entity.getApplicationId()),
                entity.getStepType(), entity.getStatus(), entity.getRequestHash(), entity.getExternalReference(),
                entity.getAttempts(), entity.getNextAttemptAt(), entity.getLastErrorCode(), entity.getStartedAt(),
                entity.getCompletedAt(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
