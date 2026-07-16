package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class OnboardingServiceAdapterTest {

    @Test
    void resolvesCompletionThroughTheInternalBodyContractWithoutPuttingTheSubjectInTheUrl() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "status": "COMPLETED",
                              "updatedAt": "2026-07-13T12:00:00Z"
                            }
                            """)
                    .build());
        }).build();
        OnboardingServiceAdapter adapter = new OnboardingServiceAdapter(webClient);

        var result = adapter.getCompletionStatus("keycloak-user-id", "internal-token");

        assertThat(result.status()).isEqualTo(OnboardingState.COMPLETED);
        assertThat(capturedRequest.get().method().name()).isEqualTo("POST");
        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://onboarding-service/internal/onboarding/completion-status")
                .doesNotContain("keycloak-user-id");
        assertThat(capturedRequest.get().headers().getFirst("Authorization"))
                .isEqualTo("Bearer internal-token");
    }

    @Test
    void forwardsTheCredentialInvitationIdempotencyKey() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.ACCEPTED)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("""
                            {
                              "applicationId": "c54ac857-4b23-4b95-8803-fc7ef5fcd675",
                              "status": "CREDENTIAL_SETUP_PENDING",
                              "submittedAt": "2026-07-10T12:00:00Z",
                              "updatedAt": "2026-07-10T12:00:00Z"
                            }
                            """)
                    .build());
        }).build();
        OnboardingServiceAdapter adapter = new OnboardingServiceAdapter(webClient);

        adapter.resendCredentialInvitation(
                "continuation-token",
                "onboarding-resend-01",
                "internal-token"
        );

        assertThat(capturedRequest.get().headers().getFirst("Idempotency-Key"))
                .isEqualTo("onboarding-resend-01");
        assertThat(capturedRequest.get().headers().getFirst("Authorization"))
                .isEqualTo("Bearer internal-token");
    }
}
