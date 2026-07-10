package com.fedebacelar.bank.homebanking.bff.application.port.out;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import java.util.UUID;

public interface OnboardingServicePort {

    OnboardingApplication startApplication(String email, String accessToken);

    OnboardingContinuation consumeMagicLink(String token, String accessToken);

    OnboardingSession validateContinuation(String token, String accessToken);

    OnboardingApplicantData saveApplicantData(String continuationToken, OnboardingApplicantData applicantData, String accessToken);

    OnboardingDocumentReference saveDocumentReference(String continuationToken, String category, UUID documentId, String accessToken);

    OnboardingTermsAcceptance acceptTerms(String continuationToken, boolean accepted, String termsVersion, String accessToken);
}
