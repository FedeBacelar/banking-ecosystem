package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingReviewCheckEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingReviewCheckJpaRepository extends JpaRepository<OnboardingReviewCheckEntity, String> {
    List<OnboardingReviewCheckEntity> findByApplicationIdOrderByCreatedAtAsc(String applicationId);
}
