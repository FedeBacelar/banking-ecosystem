package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import java.util.UUID;

public interface RetryOnboardingWorkflowUseCase {
    OnboardingApplicationDetails retryReview(UUID applicationId);
    OnboardingApplicationDetails retryProvisioning(UUID applicationId);
}
