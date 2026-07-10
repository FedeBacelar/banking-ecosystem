package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingIncompleteException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitOnboardingApplicationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingApplicantDataRepositoryPort applicants = mock(OnboardingApplicantDataRepositoryPort.class);
    private final OnboardingDocumentReferenceRepositoryPort documents = mock(OnboardingDocumentReferenceRepositoryPort.class);
    private final OnboardingTermsAcceptanceRepositoryPort terms = mock(OnboardingTermsAcceptanceRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final TokenHashingPort hashing = mock(TokenHashingPort.class);
    private final SubmitOnboardingApplicationService service = new SubmitOnboardingApplicationService(
            applications, applicants, documents, terms, history, workItems, hashing,
            Clock.fixed(NOW, ZoneOffset.UTC), "ONBOARDING_TERMS_AR_V1"
    );
    private OnboardingApplication application;

    @BeforeEach
    void setUp() {
        application = OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plusSeconds(86400), NOW.minusSeconds(60)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(30));
        when(hashing.hash("continuation-token")).thenReturn("continuation-hash");
        when(applications.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsIncompleteApplicationWithoutChangingState() {
        assertThatThrownBy(() -> service.submit(new SubmitOnboardingCommand("continuation-token")))
                .isInstanceOfSatisfying(OnboardingIncompleteException.class, exception ->
                        assertThat(exception.missingSections())
                                .containsExactlyInAnyOrder("APPLICANT_DATA", "DNI_FRONT", "DNI_BACK", "TERMS"));

        verify(applications, never()).save(any());
        verify(workItems, never()).save(any());
    }

    @Test
    void submitsAndCreatesAutoReviewWorkAtomically() {
        completeApplicationData();
        when(workItems.findByApplicationIdAndJobType(application.id(), WorkflowJobType.AUTO_REVIEW))
                .thenReturn(Optional.empty());

        var result = service.submit(new SubmitOnboardingCommand("continuation-token"));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.SUBMITTED);
        assertThat(result.submittedAt()).isEqualTo(NOW);
        verify(history).save(any());
        verify(workItems).save(argThatWorkItem(WorkflowJobType.AUTO_REVIEW));
    }

    @Test
    void repeatedSubmitReturnsCurrentStateWithoutDuplicatingWork() {
        OnboardingApplication submitted = application.submit(NOW.minusSeconds(5));
        when(applications.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(submitted));

        var result = service.submit(new SubmitOnboardingCommand("continuation-token"));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.SUBMITTED);
        verify(workItems, never()).save(any());
        verify(history, never()).save(any());
    }

    private void completeApplicationData() {
        when(applicants.findByApplicationId(application.id())).thenReturn(Optional.of(ApplicantData.create(
                application.id(), "Federico", null, "Bacelar", LocalDate.of(1990, 1, 1), "AR",
                ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1), "+5491111111111",
                "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR", NOW
        )));
        when(documents.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_FRONT))
                .thenReturn(Optional.of(OnboardingDocumentReference.create(
                        application.id(), OnboardingDocumentCategory.DNI_FRONT, java.util.UUID.randomUUID(), NOW)));
        when(documents.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_BACK))
                .thenReturn(Optional.of(OnboardingDocumentReference.create(
                        application.id(), OnboardingDocumentCategory.DNI_BACK, java.util.UUID.randomUUID(), NOW)));
        when(terms.findByApplicationId(application.id())).thenReturn(Optional.of(
                OnboardingTermsAcceptance.accept(application.id(), "ONBOARDING_TERMS_AR_V1", NOW)));
    }

    private com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem argThatWorkItem(WorkflowJobType type) {
        return org.mockito.ArgumentMatchers.argThat(item -> item.jobType() == type
                && item.applicationId().equals(application.id()));
    }
}
