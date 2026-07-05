package com.fedebacelar.bank.homebanking.bff.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingFlowServiceTest {

    private final OnboardingServicePort onboardingServicePort = mock(OnboardingServicePort.class);
    private final GetInternalAccessTokenPort getInternalAccessTokenPort = mock(GetInternalAccessTokenPort.class);
    private final OnboardingFlowService service = new OnboardingFlowService(
            onboardingServicePort,
            getInternalAccessTokenPort
    );

    @Test
    void shouldStartApplicationUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingApplication application = new OnboardingApplication(
                UUID.randomUUID(),
                "applicant@example.com",
                "EMAIL_VERIFICATION_PENDING",
                Instant.now().plusSeconds(1800),
                null,
                null,
                Instant.now().plusSeconds(86400),
                Instant.now(),
                Instant.now()
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.startApplication("applicant@example.com", accessToken)).thenReturn(application);

        OnboardingApplication result = service.startApplication("applicant@example.com");

        assertThat(result).isEqualTo(application);
    }

    @Test
    void shouldConsumeMagicLinkUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingContinuation continuation = new OnboardingContinuation(
                UUID.randomUUID(),
                "IN_PROGRESS",
                "continuation-token",
                Instant.now().plusSeconds(7200)
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.consumeMagicLink("magic-token", accessToken)).thenReturn(continuation);

        OnboardingContinuation result = service.consumeMagicLink("magic-token");

        assertThat(result).isEqualTo(continuation);
    }

    @Test
    void shouldReturnAnonymousSessionWhenContinuationCookieIsMissing() {
        OnboardingSession result = service.getSession(null);

        assertThat(result.active()).isFalse();
        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort);
    }

    @Test
    void shouldValidateContinuationUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingSession session = OnboardingSession.active(
                UUID.randomUUID(),
                "IN_PROGRESS",
                Instant.now().plusSeconds(7200)
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.validateContinuation("continuation-token", accessToken)).thenReturn(session);

        OnboardingSession result = service.getSession("continuation-token");

        assertThat(result).isEqualTo(session);
        verify(onboardingServicePort).validateContinuation("continuation-token", accessToken);
    }
}
