package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding;

import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ApplicantDataResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.AcceptTermsRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.DocumentReferenceResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.SaveApplicantDataRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.SaveDocumentReferenceRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.TermsAcceptanceResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.SubmitOnboardingRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingSubmissionResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ValidateContinuationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ValidateContinuationRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OnboardingServiceAdapter implements OnboardingServicePort {

    private final WebClient webClient;

    public OnboardingServiceAdapter(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public OnboardingApplication startApplication(String email, String accessToken) {
        OnboardingApplicationResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/applications")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new StartOnboardingApplicationRequest(email))
                .retrieve()
                .bodyToMono(OnboardingApplicationResponse.class)
                .block();

        return response.toDomain();
    }

    @Override
    public OnboardingContinuation consumeMagicLink(String token, String accessToken) {
        OnboardingContinuationResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/magic-links/consume")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new ConsumeMagicLinkRequest(token))
                .retrieve()
                .bodyToMono(OnboardingContinuationResponse.class)
                .block();

        return response.toContinuation();
    }

    @Override
    public OnboardingSession validateContinuation(String token, String accessToken) {
        ValidateContinuationResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/validate")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new ValidateContinuationRequest(token))
                .retrieve()
                .bodyToMono(ValidateContinuationResponse.class)
                .block();

        return response.toSession();
    }

    @Override
    public OnboardingApplicantData saveApplicantData(String continuationToken, OnboardingApplicantData applicantData, String accessToken) {
        ApplicantDataResponse response = webClient.put()
                .uri("http://onboarding-service/internal/onboarding/continuations/applicant-data")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(SaveApplicantDataRequest.from(continuationToken, applicantData))
                .retrieve()
                .bodyToMono(ApplicantDataResponse.class)
                .block();

        return response.toDomain();
    }

    @Override
    public OnboardingDocumentReference saveDocumentReference(String continuationToken, String category, UUID documentId, String accessToken) {
        DocumentReferenceResponse response = webClient.put()
                .uri("http://onboarding-service/internal/onboarding/continuations/documents/{category}", category)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new SaveDocumentReferenceRequest(continuationToken, documentId))
                .retrieve()
                .bodyToMono(DocumentReferenceResponse.class)
                .block();

        return response.toDomain();
    }

    @Override
    public OnboardingTermsAcceptance acceptTerms(String continuationToken, boolean accepted, String termsVersion, String accessToken) {
        TermsAcceptanceResponse response = webClient.put()
                .uri("http://onboarding-service/internal/onboarding/continuations/terms")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new AcceptTermsRequest(continuationToken, accepted, termsVersion))
                .retrieve()
                .bodyToMono(TermsAcceptanceResponse.class)
                .block();

        return response.toDomain();
    }

    @Override
    public OnboardingSubmission submit(String continuationToken, String accessToken) {
        OnboardingSubmissionResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/submissions")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new SubmitOnboardingRequest(continuationToken))
                .retrieve()
                .bodyToMono(OnboardingSubmissionResponse.class)
                .block();
        return response.toDomain();
    }

    @Override
    public OnboardingSubmission resendCredentialInvitation(String continuationToken, String accessToken) {
        OnboardingSubmissionResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/credential-invitations/resend")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new SubmitOnboardingRequest(continuationToken))
                .retrieve()
                .bodyToMono(OnboardingSubmissionResponse.class)
                .block();
        return response.toDomain();
    }
}
