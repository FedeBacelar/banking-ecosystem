package com.fedebacelar.bank.onboarding.application.mapper;

import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;

public final class OnboardingApplicationDetailsMapper {

    private OnboardingApplicationDetailsMapper() {
    }

    public static OnboardingApplicationDetails toDetails(OnboardingApplication application) {
        return new OnboardingApplicationDetails(
                application.id(),
                application.email(),
                application.status(),
                application.magicLinkExpiresAt(),
                application.magicLinkConsumedAt(),
                application.emailVerifiedAt(),
                application.continuationExpiresAt(),
                application.expiresAt(),
                application.createdAt(),
                application.updatedAt()
        );
    }
}
