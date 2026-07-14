package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingProvisioningStepRepositoryPort {
    OnboardingProvisioningStep save(OnboardingProvisioningStep step);
    Optional<OnboardingProvisioningStep> findByApplicationIdAndStepType(UUID applicationId, ProvisioningStepType type);
    Optional<OnboardingProvisioningStep> findByTypeStatusAndExternalReference(
            ProvisioningStepType type,
            ProvisioningStepStatus status,
            String externalReference
    );
    List<OnboardingProvisioningStep> findByApplicationId(UUID applicationId);
}
