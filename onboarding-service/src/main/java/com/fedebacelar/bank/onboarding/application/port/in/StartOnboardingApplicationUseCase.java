package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;

public interface StartOnboardingApplicationUseCase {

    OnboardingApplicationDetails start(StartOnboardingApplicationCommand command);
}
