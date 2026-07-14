package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.view.OnboardingCompletionDetails;

public interface GetOnboardingCompletionStatusUseCase {

    OnboardingCompletionDetails getByKeycloakSubject(String keycloakSubject);
}
