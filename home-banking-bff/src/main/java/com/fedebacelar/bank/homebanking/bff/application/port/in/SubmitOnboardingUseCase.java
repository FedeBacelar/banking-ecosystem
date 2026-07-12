package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;

public interface SubmitOnboardingUseCase {
    OnboardingSubmission submit(
            String continuationToken,
            OnboardingApplicantData applicantData,
            boolean termsAccepted,
            OnboardingFile dniFront,
            OnboardingFile dniBack
    );
}
