package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionStatus;

public interface GetOnboardingCompletionStatusUseCase {

    OnboardingCompletionStatus getForKeycloakSubject(String keycloakSubject);
}
