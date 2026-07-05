package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;

public interface ValidateContinuationUseCase {

    OnboardingApplicationDetails validate(ValidateContinuationCommand command);
}
