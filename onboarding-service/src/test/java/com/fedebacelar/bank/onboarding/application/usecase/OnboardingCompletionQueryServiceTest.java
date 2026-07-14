package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingCompletionNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OnboardingCompletionQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-13T12:00:00Z");
    private static final String KEYCLOAK_SUBJECT = "keycloak-user-id";

    private final OnboardingProvisioningStepRepositoryPort steps =
            mock(OnboardingProvisioningStepRepositoryPort.class);
    private final OnboardingApplicationRepositoryPort applications =
            mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingCompletionQueryService service =
            new OnboardingCompletionQueryService(steps, applications);

    @Test
    void resolvesTheApplicationFromTheSucceededKeycloakUserReference() {
        OnboardingApplication application = credentialSetupPendingApplication();
        OnboardingProvisioningStep userStep = OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, NOW.minusSeconds(60)
        ).succeed(KEYCLOAK_SUBJECT, NOW.minusSeconds(30));
        when(steps.findByTypeStatusAndExternalReference(
                ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
                ProvisioningStepStatus.SUCCEEDED,
                KEYCLOAK_SUBJECT
        )).thenReturn(Optional.of(userStep));
        when(applications.findById(application.id())).thenReturn(Optional.of(application));

        var result = service.getByKeycloakSubject(KEYCLOAK_SUBJECT);

        assertThat(result.status()).isEqualTo(application.status());
        assertThat(result.updatedAt()).isEqualTo(application.updatedAt());
    }

    @Test
    void returnsAStableNotFoundResultWithoutFallingBackToEmailOrContinuation() {
        when(steps.findByTypeStatusAndExternalReference(
                ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
                ProvisioningStepStatus.SUCCEEDED,
                KEYCLOAK_SUBJECT
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByKeycloakSubject(KEYCLOAK_SUBJECT))
                .isInstanceOf(OnboardingCompletionNotFoundException.class)
                .hasMessageNotContaining(KEYCLOAK_SUBJECT);

        verify(steps).findByTypeStatusAndExternalReference(
                ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
                ProvisioningStepStatus.SUCCEEDED,
                KEYCLOAK_SUBJECT
        );
    }

    private OnboardingApplication credentialSetupPendingApplication() {
        return OnboardingApplication.start(
                "person@example.com", "magic-hash", NOW.plusSeconds(1800), NOW.plusSeconds(86400),
                NOW.minusSeconds(600)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(500))
                .submit(NOW.minusSeconds(400))
                .startAutomatedReview(NOW.minusSeconds(300))
                .approve(NOW.minusSeconds(200))
                .startProvisioning(NOW.minusSeconds(100))
                .markCredentialSetupPending(NOW);
    }
}
