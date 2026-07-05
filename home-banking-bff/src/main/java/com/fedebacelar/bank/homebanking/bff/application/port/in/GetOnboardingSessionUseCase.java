package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;

public interface GetOnboardingSessionUseCase {

    OnboardingSession getSession(String continuationToken);
}
