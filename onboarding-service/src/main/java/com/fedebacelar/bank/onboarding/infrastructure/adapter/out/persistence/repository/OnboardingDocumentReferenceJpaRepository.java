package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingDocumentReferenceEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingDocumentReferenceJpaRepository extends JpaRepository<OnboardingDocumentReferenceEntity, String> {

    Optional<OnboardingDocumentReferenceEntity> findByApplicationIdAndCategory(String applicationId, OnboardingDocumentCategory category);
}
