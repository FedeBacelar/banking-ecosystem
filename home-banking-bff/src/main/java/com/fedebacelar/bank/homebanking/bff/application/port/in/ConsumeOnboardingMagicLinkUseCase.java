package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;

public interface ConsumeOnboardingMagicLinkUseCase {

    OnboardingContinuation consumeMagicLink(String token);
}
