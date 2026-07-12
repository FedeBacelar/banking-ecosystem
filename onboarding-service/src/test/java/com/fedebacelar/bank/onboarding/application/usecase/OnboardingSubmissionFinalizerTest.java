package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingSubmissionFinalizerTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingApplicantDataRepositoryPort applicantData = mock(OnboardingApplicantDataRepositoryPort.class);
    private final OnboardingDocumentReferenceRepositoryPort documentReferences = mock(OnboardingDocumentReferenceRepositoryPort.class);
    private final OnboardingTermsAcceptanceRepositoryPort terms = mock(OnboardingTermsAcceptanceRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final TokenHashingPort hashing = mock(TokenHashingPort.class);
    private final OnboardingReviewPolicyPort reviewPolicy = mock(OnboardingReviewPolicyPort.class);
    private final OnboardingSubmissionFinalizer finalizer = new OnboardingSubmissionFinalizer(
            applications,
            applicantData,
            documentReferences,
            terms,
            history,
            workItems,
            hashing,
            reviewPolicy,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @BeforeEach
    void setUp() {
        when(hashing.hash("continuation-token")).thenReturn("continuation-hash");
        when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(applicantData.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentReferences.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(terms.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(history.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workItems.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        OnboardingReviewPolicyPort.ReviewPolicy policy = mock(OnboardingReviewPolicyPort.ReviewPolicy.class);
        when(policy.requiredTermsVersion()).thenReturn("ONBOARDING_TERMS_AR_V1");
        when(reviewPolicy.policy("AR_DNI_SAVINGS_V1")).thenReturn(policy);
    }

    @Test
    void shouldLockAndAtomicallyPersistTheCompleteSubmission() {
        OnboardingApplication application = inProgressApplication();
        SubmitOnboardingCommand command = command();
        when(applications.findByContinuationTokenHashForUpdate("continuation-hash"))
                .thenReturn(Optional.of(application));
        when(workItems.findByApplicationIdAndJobType(application.id(), WorkflowJobType.AUTO_REVIEW))
                .thenReturn(Optional.empty());

        var result = finalizer.complete(command, UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.SUBMITTED);
        verify(applications).findByContinuationTokenHashForUpdate("continuation-hash");
        verify(applicantData).save(any());
        verify(documentReferences, org.mockito.Mockito.times(2)).save(any());
        verify(terms).save(any());
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                item -> item.jobType() == WorkflowJobType.AUTO_REVIEW
        ));
    }

    @Test
    void shouldReturnCurrentPostSubmissionStateWhenAConcurrentSubmitAlreadyCompleted() {
        OnboardingApplication submitted = inProgressApplication()
                .submit(NOW.minusSeconds(6))
                .startAutomatedReview(NOW.minusSeconds(5))
                .approve(NOW.minusSeconds(4))
                .startProvisioning(NOW.minusSeconds(3))
                .markCredentialSetupPending(NOW.minusSeconds(2))
                .failCredentialSetup(NOW.minusSeconds(1));
        when(applications.findByContinuationTokenHashForUpdate("continuation-hash"))
                .thenReturn(Optional.of(submitted));

        var result = finalizer.complete(command(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_FAILED);
        verify(applicantData, never()).save(any());
        verify(documentReferences, never()).save(any());
        verify(terms, never()).save(any());
        verify(workItems, never()).save(any());
    }

    private OnboardingApplication inProgressApplication() {
        return OnboardingApplication.start(
                "person@example.com", "magic-hash", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)),
                NOW.minusSeconds(60)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(30));
    }

    private SubmitOnboardingCommand command() {
        OnboardingDocumentUpload upload = new OnboardingDocumentUpload(
                "dni.png", "image/png", 1, () -> new ByteArrayInputStream(new byte[]{1})
        );
        return new SubmitOnboardingCommand(
                "continuation-token", "Federico", null, "Bacelar", LocalDate.of(1990, 1, 1),
                "AR", ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1),
                "+5491111111111", "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR",
                true, upload, upload
        );
    }
}
