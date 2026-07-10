package com.fedebacelar.bank.onboarding.application.mapper;

import com.fedebacelar.bank.onboarding.application.view.TermsAcceptanceDetails;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;

public final class TermsAcceptanceDetailsMapper {

    private TermsAcceptanceDetailsMapper() {
    }

    public static TermsAcceptanceDetails toDetails(OnboardingTermsAcceptance acceptance) {
        return new TermsAcceptanceDetails(
                acceptance.applicationId(),
                acceptance.termsVersion(),
                acceptance.acceptedAt(),
                acceptance.createdAt(),
                acceptance.updatedAt()
        );
    }
}
