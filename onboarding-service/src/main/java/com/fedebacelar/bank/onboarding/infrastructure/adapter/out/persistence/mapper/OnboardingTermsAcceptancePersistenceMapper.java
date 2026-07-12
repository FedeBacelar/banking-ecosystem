package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingTermsAcceptanceEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingTermsAcceptancePersistenceMapper {

    public OnboardingTermsAcceptanceEntity toEntity(OnboardingTermsAcceptance acceptance) {
        OnboardingTermsAcceptanceEntity entity = new OnboardingTermsAcceptanceEntity();
        entity.setApplicationId(acceptance.applicationId().toString());
        entity.setTermsVersion(acceptance.termsVersion());
        entity.setAcceptedAt(acceptance.acceptedAt());
        entity.setCreatedAt(acceptance.createdAt());
        entity.setUpdatedAt(acceptance.updatedAt());
        entity.setVersion(acceptance.version());
        return entity;
    }

    public OnboardingTermsAcceptance toDomain(OnboardingTermsAcceptanceEntity entity) {
        return new OnboardingTermsAcceptance(
                UUID.fromString(entity.getApplicationId()),
                entity.getTermsVersion(),
                entity.getAcceptedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
