package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingDocumentReferenceEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingDocumentReferencePersistenceMapper {

    public OnboardingDocumentReferenceEntity toEntity(OnboardingDocumentReference reference) {
        OnboardingDocumentReferenceEntity entity = new OnboardingDocumentReferenceEntity();
        entity.setId(reference.id().toString());
        entity.setApplicationId(reference.applicationId().toString());
        entity.setCategory(reference.category());
        entity.setDocumentId(reference.documentId().toString());
        entity.setCreatedAt(reference.createdAt());
        entity.setUpdatedAt(reference.updatedAt());
        entity.setVersion(reference.version());
        return entity;
    }

    public OnboardingDocumentReference toDomain(OnboardingDocumentReferenceEntity entity) {
        return new OnboardingDocumentReference(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getApplicationId()),
                entity.getCategory(),
                UUID.fromString(entity.getDocumentId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
