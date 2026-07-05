package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import java.util.UUID;

public interface GetOnboardingApplicationUseCase {

    OnboardingApplicationDetails get(UUID applicationId);
}
