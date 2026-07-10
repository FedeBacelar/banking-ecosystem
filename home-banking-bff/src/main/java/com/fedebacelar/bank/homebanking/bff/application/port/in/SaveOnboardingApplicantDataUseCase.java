package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;

public interface SaveOnboardingApplicantDataUseCase {

    OnboardingApplicantData saveApplicantData(String continuationToken, OnboardingApplicantData applicantData);
}
