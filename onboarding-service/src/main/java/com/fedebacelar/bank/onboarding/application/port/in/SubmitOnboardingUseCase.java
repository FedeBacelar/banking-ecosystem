package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;

public interface SubmitOnboardingUseCase {
    OnboardingSubmissionDetails submit(SubmitOnboardingCommand command);
}
