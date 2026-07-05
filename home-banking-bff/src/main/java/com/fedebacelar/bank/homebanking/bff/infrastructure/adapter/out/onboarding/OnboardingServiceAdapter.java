package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding;

import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding.dto.ValidateContinuationRequest;
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
        OnboardingContinuationResponse response = webClient.post()
                .uri("http://onboarding-service/internal/onboarding/continuations/validate")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(new ValidateContinuationRequest(token))
                .retrieve()
                .bodyToMono(OnboardingContinuationResponse.class)
                .block();

        return response.toSession();
    }
}
