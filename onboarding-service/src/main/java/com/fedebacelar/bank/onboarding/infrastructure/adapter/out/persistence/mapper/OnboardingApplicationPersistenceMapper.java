package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicationEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApplicationPersistenceMapper {

    public OnboardingApplicationEntity toEntity(OnboardingApplication application) {
        OnboardingApplicationEntity entity = new OnboardingApplicationEntity();
        entity.setId(application.id().toString());
        entity.setEmail(application.email());
        entity.setStatus(application.status());
        entity.setReviewMode(application.reviewMode());
        entity.setReviewPolicyVersion(application.reviewPolicyVersion());
        entity.setMagicLinkTokenHash(application.magicLinkTokenHash());
        entity.setMagicLinkExpiresAt(application.magicLinkExpiresAt());
        entity.setMagicLinkConsumedAt(application.magicLinkConsumedAt());
        entity.setEmailVerifiedAt(application.emailVerifiedAt());
        entity.setContinuationTokenHash(application.continuationTokenHash());
        entity.setContinuationExpiresAt(application.continuationExpiresAt());
        entity.setExpiresAt(application.expiresAt());
        entity.setSubmittedAt(application.submittedAt());
        entity.setDecidedAt(application.decidedAt());
        entity.setCreatedAt(application.createdAt());
        entity.setUpdatedAt(application.updatedAt());
        entity.setVersion(application.version());
        return entity;
    }

    public OnboardingApplication toDomain(OnboardingApplicationEntity entity) {
        return new OnboardingApplication(
                UUID.fromString(entity.getId()),
                entity.getEmail(),
                entity.getStatus(),
                entity.getReviewMode(),
                entity.getReviewPolicyVersion(),
                entity.getMagicLinkTokenHash(),
                entity.getMagicLinkExpiresAt(),
                entity.getMagicLinkConsumedAt(),
                entity.getEmailVerifiedAt(),
                entity.getContinuationTokenHash(),
                entity.getContinuationExpiresAt(),
                entity.getExpiresAt(),
                entity.getSubmittedAt(),
                entity.getDecidedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
