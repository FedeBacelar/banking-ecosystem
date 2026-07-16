package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.port.out.CustomerDuplicateLookupPort;
import com.fedebacelar.bank.onboarding.application.port.out.DocumentValidationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewCheckRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckOutcome;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingReviewCheck;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.config.OnboardingReviewProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class AutoReviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingApplicantDataRepositoryPort applicants = mock(OnboardingApplicantDataRepositoryPort.class);
    private final OnboardingDocumentReferenceRepositoryPort documents = mock(OnboardingDocumentReferenceRepositoryPort.class);
    private final OnboardingTermsAcceptanceRepositoryPort terms = mock(OnboardingTermsAcceptanceRepositoryPort.class);
    private final OnboardingReviewCheckRepositoryPort checks = mock(OnboardingReviewCheckRepositoryPort.class);
    private final OnboardingStatusHistoryRepositoryPort history = mock(OnboardingStatusHistoryRepositoryPort.class);
    private final OnboardingWorkItemRepositoryPort workItems = mock(OnboardingWorkItemRepositoryPort.class);
    private final CustomerDuplicateLookupPort customers = mock(CustomerDuplicateLookupPort.class);
    private final DocumentValidationPort documentValidation = mock(DocumentValidationPort.class);
    private final OnboardingUniquenessReservationPort reservations = mock(OnboardingUniquenessReservationPort.class);
    private final OnboardingReviewProperties properties = new OnboardingReviewProperties();
    private final TransactionTemplate transactions = mock(TransactionTemplate.class);
    private final OnboardingTelemetryPort telemetry = mock(OnboardingTelemetryPort.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AtomicReference<OnboardingApplication> persistedApplication = new AtomicReference<>();
    private final List<OnboardingReviewCheck> persistedChecks = new ArrayList<>();
    private AutoReviewService service;
    private OnboardingWorkItem claimedWorkItem;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(transactions.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
        doAnswer(invocation -> {
            ((Consumer<TransactionStatus>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());

        OnboardingApplication submitted = OnboardingApplication.start(
                "person@example.com", "magic", NOW.plusSeconds(1800), NOW.plus(Duration.ofDays(15)), NOW.minusSeconds(60)
        ).verifyEmail("continuation", NOW.plusSeconds(7200), NOW.minusSeconds(50)).submit(NOW.minusSeconds(40));
        persistedApplication.set(submitted);
        when(applications.findById(submitted.id())).thenAnswer(invocation -> Optional.of(persistedApplication.get()));
        when(applications.save(any())).thenAnswer(invocation -> {
            OnboardingApplication saved = invocation.getArgument(0);
            persistedApplication.set(saved);
            return saved;
        });
        ApplicantData applicant = applicant(submitted, LocalDate.of(1990, 1, 1));
        when(applicants.findByApplicationId(submitted.id())).thenReturn(Optional.of(applicant));
        when(documents.findByApplicationIdAndCategory(submitted.id(), OnboardingDocumentCategory.DNI_FRONT))
                .thenReturn(Optional.of(OnboardingDocumentReference.create(
                        submitted.id(), OnboardingDocumentCategory.DNI_FRONT, java.util.UUID.randomUUID(), NOW)));
        when(documents.findByApplicationIdAndCategory(submitted.id(), OnboardingDocumentCategory.DNI_BACK))
                .thenReturn(Optional.of(OnboardingDocumentReference.create(
                        submitted.id(), OnboardingDocumentCategory.DNI_BACK, java.util.UUID.randomUUID(), NOW)));
        when(terms.findByApplicationId(submitted.id())).thenReturn(Optional.of(
                OnboardingTermsAcceptance.accept(submitted.id(), properties.getRequiredTermsVersion(), NOW)));
        when(reservations.tryAcquire(any(), any(), any(), any())).thenReturn(true);
        when(documentValidation.isStoredOnboardingDocument(any(), any(), any())).thenReturn(true);
        when(checks.save(any())).thenAnswer(invocation -> {
            OnboardingReviewCheck check = invocation.getArgument(0);
            persistedChecks.add(check);
            return check;
        });
        when(workItems.findByApplicationIdAndJobType(submitted.id(), WorkflowJobType.PROVISIONING))
                .thenReturn(Optional.empty());
        claimedWorkItem = OnboardingWorkItem.pending(submitted.id(), WorkflowJobType.AUTO_REVIEW, NOW.minusSeconds(30))
                .claim(NOW.minusSeconds(20), Duration.ofMinutes(2));
        service = new AutoReviewService(applications, applicants, documents, terms, checks, history, workItems,
                customers, documentValidation, reservations, properties, transactions, telemetry, clock);
    }

    @Test
    void approvesAndPersistsSimulatedChecksWithExplicitProvenance() {
        service.execute(claimedWorkItem);

        assertThat(persistedApplication.get().status()).isEqualTo(OnboardingApplicationStatus.APPROVED);
        assertThat(persistedChecks).hasSize(7);
        assertThat(persistedChecks.stream().filter(check -> check.executionMode() == ReviewCheckExecutionMode.SIMULATED))
                .hasSize(3)
                .allSatisfy(check -> {
                    assertThat(check.outcome()).isEqualTo(ReviewCheckOutcome.PASSED);
                    assertThat(check.provider()).isEqualTo("SIMULATOR");
                    assertThat(check.reasonCode()).isEqualTo("SIMULATED_APPROVAL");
                });
        verify(workItems).save(org.mockito.ArgumentMatchers.argThat(
                item -> item.jobType() == WorkflowJobType.PROVISIONING));
        verify(reservations, never()).releaseByApplicationId(any(), any());
    }

    @Test
    void rejectsARealNegativeEligibilityDecisionAndReleasesReservations() {
        OnboardingApplication submitted = persistedApplication.get();
        when(applicants.findByApplicationId(submitted.id()))
                .thenReturn(Optional.of(applicant(submitted, LocalDate.of(2012, 1, 1))));

        service.execute(claimedWorkItem);

        assertThat(persistedApplication.get().status()).isEqualTo(OnboardingApplicationStatus.REJECTED);
        assertThat(persistedChecks)
                .filteredOn(check -> "BASIC_ELIGIBILITY_FAILED".equals(check.reasonCode()))
                .singleElement()
                .extracting(OnboardingReviewCheck::outcome)
                .isEqualTo(ReviewCheckOutcome.FAILED);
        verify(reservations).releaseByApplicationId(submitted.id(), NOW);
        verify(workItems, never()).findByApplicationIdAndJobType(submitted.id(), WorkflowJobType.PROVISIONING);
        verify(telemetry).recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.REJECTED);
        verify(telemetry).recordWorkOutcome(
                OnboardingTelemetryPort.WorkType.AUTO_REVIEW,
                OnboardingTelemetryPort.WorkOutcome.SUCCEEDED
        );
    }

    private ApplicantData applicant(OnboardingApplication application, LocalDate birthDate) {
        return ApplicantData.create(application.id(), "Federico", null, "Bacelar", birthDate, "AR",
                ApplicantDocumentType.DNI, "30111222", "AR", LocalDate.of(2030, 1, 1), "+5491111111111",
                "Calle", "1", "Ciudad", "Buenos Aires", "1000", "AR", NOW);
    }
}
