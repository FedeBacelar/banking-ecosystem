package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;

public interface ConsumeMagicLinkUseCase {

    OnboardingContinuationDetails consume(ConsumeMagicLinkCommand command);
}
