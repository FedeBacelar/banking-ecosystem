package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingProvisioningStepEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OnboardingProvisioningStepJpaRepository extends JpaRepository<OnboardingProvisioningStepEntity, String> {
    Optional<OnboardingProvisioningStepEntity> findByApplicationIdAndStepType(String applicationId, ProvisioningStepType type);
    List<OnboardingProvisioningStepEntity> findByApplicationIdOrderByCreatedAtAsc(String applicationId);
}
