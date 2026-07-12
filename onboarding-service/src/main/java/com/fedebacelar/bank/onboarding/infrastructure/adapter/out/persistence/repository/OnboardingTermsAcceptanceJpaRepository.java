package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingTermsAcceptanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingTermsAcceptanceJpaRepository extends JpaRepository<OnboardingTermsAcceptanceEntity, String> {
}
