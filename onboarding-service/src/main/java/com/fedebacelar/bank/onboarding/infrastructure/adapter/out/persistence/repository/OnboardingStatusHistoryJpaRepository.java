package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingStatusHistoryJpaRepository extends JpaRepository<OnboardingStatusHistoryEntity, String> {
}
