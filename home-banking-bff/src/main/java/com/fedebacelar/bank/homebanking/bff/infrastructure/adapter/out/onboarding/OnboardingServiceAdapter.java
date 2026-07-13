package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding;

import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingSubmissionResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.SubmitOnboardingRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ValidateContinuationRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ValidateContinuationResponse;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.AbstractResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OnboardingServiceAdapter implements OnboardingServicePort {

    private final WebClient webClient;

    public OnboardingServiceAdapter(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public void startApplication(String email, String accessToken) {
        webClient.post()
                .uri("http://onboarding-service/internal/onboarding/applications")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new StartOnboardingApplicationRequest(email))
                .retrieve()
                .toBodilessEntity()
                .block();
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
    public OnboardingSubmission submit(
            String continuationToken,
            OnboardingApplicantData applicantData,
            boolean termsAccepted,
            OnboardingFile dniFront,
            OnboardingFile dniBack,
            String accessToken
    ) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("submission", SubmitOnboardingRequest.from(continuationToken, applicantData, termsAccepted))
                .contentType(MediaType.APPLICATION_JSON);
        addFile(body, "dniFront", dniFront);
        addFile(body, "dniBack", dniBack);

        OnboardingSubmissionResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/submissions")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(OnboardingSubmissionResponse.class)
                .block();
        return response.toDomain();
    }

    @Override
    public OnboardingSubmission resendCredentialInvitation(
            String continuationToken,
            String idempotencyKey,
            String accessToken
    ) {
        OnboardingSubmissionResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/credential-invitations/resend")
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.set("Idempotency-Key", idempotencyKey);
                })
                .bodyValue(new com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ContinuationRequest(
                        continuationToken
                ))
                .retrieve()
                .bodyToMono(OnboardingSubmissionResponse.class)
                .block();
        return response.toDomain();
    }

    private void addFile(MultipartBodyBuilder body, String partName, OnboardingFile file) {
        body.part(partName, new OnboardingFileResource(file))
                .filename(file.originalFilename())
                .contentType(MediaType.parseMediaType(file.contentType()));
    }

    private static final class OnboardingFileResource extends AbstractResource {

        private final OnboardingFile file;

        private OnboardingFileResource(OnboardingFile file) {
            this.file = file;
        }

        @Override
        public String getDescription() {
            return "onboarding file " + file.originalFilename();
        }

        @Override
        public String getFilename() {
            return file.originalFilename();
        }

        @Override
        public long contentLength() {
            return file.size();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return file.openStream();
        }
    }
}
