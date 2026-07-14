package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingCompletionStatusUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingCompletionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingCompletionNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingCompletionQueryService implements GetOnboardingCompletionStatusUseCase {

    private final OnboardingProvisioningStepRepositoryPort provisioningSteps;
    private final OnboardingApplicationRepositoryPort applications;

    public OnboardingCompletionQueryService(
            OnboardingProvisioningStepRepositoryPort provisioningSteps,
            OnboardingApplicationRepositoryPort applications
    ) {
        this.provisioningSteps = provisioningSteps;
        this.applications = applications;
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingCompletionDetails getByKeycloakSubject(String keycloakSubject) {
        OnboardingApplication application = provisioningSteps.findByTypeStatusAndExternalReference(
                        ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
                        ProvisioningStepStatus.SUCCEEDED,
                        keycloakSubject
                )
                .flatMap(step -> applications.findById(step.applicationId()))
                .orElseThrow(OnboardingCompletionNotFoundException::new);

        return new OnboardingCompletionDetails(application.status(), application.updatedAt());
    }
}
