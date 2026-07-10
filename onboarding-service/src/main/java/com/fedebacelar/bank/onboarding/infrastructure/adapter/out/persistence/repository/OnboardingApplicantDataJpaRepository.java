package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingApplicantDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingApplicantDataJpaRepository extends JpaRepository<OnboardingApplicantDataEntity, String> {
}
