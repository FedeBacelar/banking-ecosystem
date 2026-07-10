package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingReviewCheck;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingReviewCheckEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingReviewCheckPersistenceMapper {
    public OnboardingReviewCheckEntity toEntity(OnboardingReviewCheck check) {
        OnboardingReviewCheckEntity entity = new OnboardingReviewCheckEntity();
        entity.setId(check.id().toString());
        entity.setApplicationId(check.applicationId().toString());
        entity.setCheckType(check.checkType());
        entity.setExecutionMode(check.executionMode());
        entity.setExecutionStatus(check.executionStatus());
        entity.setOutcome(check.outcome());
        entity.setBlocking(check.blocking());
        entity.setPolicyVersion(check.policyVersion());
        entity.setProvider(check.provider());
        entity.setReasonCode(check.reasonCode());
        entity.setAttempts(check.attempts());
        entity.setStartedAt(check.startedAt());
        entity.setCompletedAt(check.completedAt());
        entity.setCreatedAt(check.createdAt());
        entity.setUpdatedAt(check.updatedAt());
        entity.setVersion(check.version());
        return entity;
    }

    public OnboardingReviewCheck toDomain(OnboardingReviewCheckEntity entity) {
        return new OnboardingReviewCheck(UUID.fromString(entity.getId()), UUID.fromString(entity.getApplicationId()),
                entity.getCheckType(), entity.getExecutionMode(), entity.getExecutionStatus(), entity.getOutcome(),
                entity.isBlocking(), entity.getPolicyVersion(), entity.getProvider(), entity.getReasonCode(),
                entity.getAttempts(), entity.getStartedAt(), entity.getCompletedAt(), entity.getCreatedAt(),
                entity.getUpdatedAt(), entity.getVersion());
    }
}
