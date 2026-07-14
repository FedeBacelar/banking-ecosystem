package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.onboarding.TestcontainersConfiguration;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest(properties = {
        "onboarding.review.worker-batch-size=0",
        "onboarding.provisioning.worker-batch-size=0"
})
@Import(TestcontainersConfiguration.class)
class OnboardingCompletionReferencePersistenceIntegrationTest {

    @Autowired
    private OnboardingApplicationRepositoryPort applications;

    @Autowired
    private OnboardingProvisioningStepRepositoryPort steps;

    @Test
    void resolvesOnlyASucceededKeycloakUserStepByItsExternalReference() {
        Instant now = Instant.parse("2026-07-13T12:00:00Z");
        OnboardingApplication application = saveApplication(now);
        String subject = UUID.randomUUID().toString();
        steps.save(OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, now
        ).succeed(subject, now));

        var result = steps.findByTypeStatusAndExternalReference(
                ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
                ProvisioningStepStatus.SUCCEEDED,
                subject
        );

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().applicationId()).isEqualTo(application.id());
    }

    @Test
    void preventsTwoApplicationsFromReferencingTheSameKeycloakUser() {
        Instant now = Instant.parse("2026-07-13T12:00:00Z");
        String subject = UUID.randomUUID().toString();
        OnboardingApplication first = saveApplication(now);
        OnboardingApplication second = saveApplication(now.plusSeconds(1));
        steps.save(OnboardingProvisioningStep.pending(
                first.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, now
        ).succeed(subject, now));

        assertThatThrownBy(() -> steps.save(OnboardingProvisioningStep.pending(
                second.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, now
        ).succeed(subject, now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTheSameReferenceForDifferentProvisioningStepTypes() {
        Instant now = Instant.parse("2026-07-13T12:00:00Z");
        String subject = UUID.randomUUID().toString();
        OnboardingApplication application = saveApplication(now);

        steps.save(OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER, now
        ).succeed(subject, now));
        OnboardingProvisioningStep invitation = steps.save(OnboardingProvisioningStep.pending(
                application.id(), ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL, now
        ).succeed(subject, now));

        assertThat(invitation.externalReference()).isEqualTo(subject);
    }

    private OnboardingApplication saveApplication(Instant now) {
        String suffix = UUID.randomUUID().toString();
        return applications.save(OnboardingApplication.start(
                suffix + "@example.com",
                suffix.replace("-", ""),
                now.plusSeconds(1800),
                now.plusSeconds(86400),
                now
        ));
    }
}
