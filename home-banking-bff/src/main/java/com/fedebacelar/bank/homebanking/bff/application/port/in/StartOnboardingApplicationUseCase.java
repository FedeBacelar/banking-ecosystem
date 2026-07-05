package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;

public interface StartOnboardingApplicationUseCase {

    OnboardingApplication startApplication(String email);
}
