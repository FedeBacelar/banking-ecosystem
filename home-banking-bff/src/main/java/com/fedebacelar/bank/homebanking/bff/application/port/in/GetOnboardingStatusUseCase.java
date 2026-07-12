package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;

public interface GetOnboardingStatusUseCase {
    OnboardingPublicStatus getStatus(String continuationToken);
}
