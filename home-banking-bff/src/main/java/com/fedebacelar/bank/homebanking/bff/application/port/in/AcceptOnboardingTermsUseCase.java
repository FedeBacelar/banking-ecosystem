package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;

public interface AcceptOnboardingTermsUseCase {

    OnboardingTermsAcceptance acceptTerms(String continuationToken, boolean accepted, String termsVersion);
}
