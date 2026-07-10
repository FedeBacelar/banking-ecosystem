package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CredentialInvitationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void restoresInvitationStepAfterDeliveryFailureSoItCanBeRetried() {
        OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
        OnboardingProvisioningStepRepositoryPort steps = mock(OnboardingProvisioningStepRepositoryPort.class);
        CredentialProvisioningPort credentials = mock(CredentialProvisioningPort.class);
        TokenHashingPort hashing = mock(TokenHashingPort.class);
        OnboardingApplication application = credentialPendingApplication();
        OnboardingProvisioningStep invitation = OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, NOW.minusSeconds(180)
        ).start("hash", NOW.minusSeconds(170)).succeed("keycloak-user-id", NOW.minusSeconds(120));
        List<OnboardingProvisioningStep> saved = new ArrayList<>();

        when(hashing.hash("continuation-token")).thenReturn("continuation-hash");
        when(applications.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(steps.findByApplicationIdAndStepType(application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL))
                .thenReturn(Optional.of(invitation));
        when(steps.save(any())).thenAnswer(invocation -> {
            OnboardingProvisioningStep step = invocation.getArgument(0);
            saved.add(step);
            return step;
        });
        doThrow(new IllegalStateException("smtp unavailable"))
                .when(credentials).sendCredentialSetupEmail("keycloak-user-id");
        CredentialInvitationService service = new CredentialInvitationService(
                applications, steps, credentials, hashing, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(1)
        );

        assertThatThrownBy(() -> service.resend("continuation-token"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).status()).isEqualTo(ProvisioningStepStatus.RUNNING);
        assertThat(saved.get(1).status()).isEqualTo(ProvisioningStepStatus.SUCCEEDED);
        assertThat(saved.get(1).lastErrorCode()).isEqualTo("CREDENTIAL_INVITATION_DELIVERY_ERROR");
    }

    private OnboardingApplication credentialPendingApplication() {
        return OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)), NOW.minusSeconds(200)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(190))
                .submit(NOW.minusSeconds(180))
                .startAutomatedReview(NOW.minusSeconds(170))
                .approve(NOW.minusSeconds(160))
                .startProvisioning(NOW.minusSeconds(150))
                .markCredentialSetupPending(NOW.minusSeconds(120));
    }
}
