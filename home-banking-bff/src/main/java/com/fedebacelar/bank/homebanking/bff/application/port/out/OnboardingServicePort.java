package com.fedebacelar.bank.homebanking.bff.application.port.out;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;

public interface OnboardingServicePort {

    OnboardingApplication startApplication(String email, String accessToken);

    OnboardingContinuation consumeMagicLink(String token, String accessToken);

    OnboardingSession validateContinuation(String token, String accessToken);
}
