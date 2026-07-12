package com.fedebacelar.bank.homebanking.bff.application.port.out;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;

public interface OnboardingServicePort {

    void startApplication(String email, String accessToken);

    OnboardingContinuation consumeMagicLink(String token, String accessToken);

    OnboardingSession validateContinuation(String token, String accessToken);

    OnboardingSubmission submit(
            String continuationToken,
            OnboardingApplicantData applicantData,
            boolean termsAccepted,
            OnboardingFile dniFront,
            OnboardingFile dniBack,
            String accessToken
    );

    OnboardingSubmission resendCredentialInvitation(String continuationToken, String accessToken);
}
