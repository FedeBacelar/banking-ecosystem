package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.CustomerDuplicateLookupPort;
import com.fedebacelar.bank.onboarding.application.port.out.DocumentValidationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewCheckRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckOutcome;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.enums.UniquenessReservationType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingReviewCheck;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AutoReviewService {
    private static final Set<OnboardingApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING, OnboardingApplicationStatus.IN_PROGRESS,
            OnboardingApplicationStatus.SUBMITTED, OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW,
            OnboardingApplicationStatus.REVIEW_FAILED, OnboardingApplicationStatus.APPROVED,
            OnboardingApplicationStatus.PROVISIONING, OnboardingApplicationStatus.PROVISIONING_FAILED,
            OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING
    );

    private final OnboardingApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicantDataRepositoryPort applicantDataRepository;
    private final OnboardingDocumentReferenceRepositoryPort documentReferenceRepository;
    private final OnboardingTermsAcceptanceRepositoryPort termsRepository;
    private final OnboardingReviewCheckRepositoryPort checkRepository;
    private final OnboardingStatusHistoryRepositoryPort historyRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final CustomerDuplicateLookupPort customerLookup;
    private final DocumentValidationPort documentValidation;
    private final OnboardingUniquenessReservationPort reservationPort;
    private final OnboardingReviewPolicyPort reviewPolicy;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public AutoReviewService(
            OnboardingApplicationRepositoryPort applicationRepository,
            OnboardingApplicantDataRepositoryPort applicantDataRepository,
            OnboardingDocumentReferenceRepositoryPort documentReferenceRepository,
            OnboardingTermsAcceptanceRepositoryPort termsRepository,
            OnboardingReviewCheckRepositoryPort checkRepository,
            OnboardingStatusHistoryRepositoryPort historyRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            CustomerDuplicateLookupPort customerLookup,
            DocumentValidationPort documentValidation,
            OnboardingUniquenessReservationPort reservationPort,
            OnboardingReviewPolicyPort reviewPolicy,
            TransactionTemplate transactionTemplate,
            Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.applicantDataRepository = applicantDataRepository;
        this.documentReferenceRepository = documentReferenceRepository;
        this.termsRepository = termsRepository;
        this.checkRepository = checkRepository;
        this.historyRepository = historyRepository;
        this.workItemRepository = workItemRepository;
        this.customerLookup = customerLookup;
        this.documentValidation = documentValidation;
        this.reservationPort = reservationPort;
        this.reviewPolicy = reviewPolicy;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public void execute(OnboardingWorkItem workItem) {
        Instant now = Instant.now(clock);
        ReviewContext context = transactionTemplate.execute(status -> beginReview(workItem.applicationId(), now));
        if (context == null) {
            throw new IllegalStateException("Could not load onboarding review context.");
        }

        List<OnboardingReviewCheck> checks = evaluate(context, now);
        boolean rejected = checks.stream().anyMatch(check -> check.blocking() && check.outcome() == ReviewCheckOutcome.FAILED);

        transactionTemplate.executeWithoutResult(status -> finishReview(workItem, context.application(), checks, rejected, Instant.now(clock)));
    }

    public void handleTechnicalFailure(OnboardingWorkItem workItem, String errorCode) {
        Instant now = Instant.now(clock);
        transactionTemplate.executeWithoutResult(status -> {
            if (workItem.attempts() >= reviewPolicy.maxAttempts()) {
                OnboardingApplication application = requireApplication(workItem.applicationId());
                if (application.status() == OnboardingApplicationStatus.SUBMITTED
                        || application.status() == OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW) {
                    OnboardingApplication failed = applicationRepository.save(application.failReview(now));
                    saveHistory(application, failed, "AUTO_REVIEW_TECHNICAL_FAILURE", OnboardingActorType.AUTO_REVIEW, now);
                }
                workItemRepository.save(workItem.fail(errorCode, now));
                return;
            }
            workItemRepository.save(workItem.retry(errorCode, now.plus(reviewPolicy.retryDelay(workItem.attempts())), now));
        });
    }

    private ReviewContext beginReview(java.util.UUID applicationId, Instant now) {
        OnboardingApplication application = requireApplication(applicationId);
        if (application.status() == OnboardingApplicationStatus.SUBMITTED) {
            OnboardingApplication reviewing = applicationRepository.save(application.startAutomatedReview(now));
            saveHistory(application, reviewing, "AUTO_REVIEW_STARTED", OnboardingActorType.AUTO_REVIEW, now);
            application = reviewing;
        }
        if (application.status() != OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW) {
            throw new IllegalStateException("Application is not ready for AUTO review.");
        }
        ApplicantData applicant = applicantDataRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException("Applicant data disappeared after submission."));
        OnboardingDocumentReference front = documentReferenceRepository
                .findByApplicationIdAndCategory(applicationId, OnboardingDocumentCategory.DNI_FRONT)
                .orElseThrow(() -> new IllegalStateException("DNI front reference disappeared after submission."));
        OnboardingDocumentReference back = documentReferenceRepository
                .findByApplicationIdAndCategory(applicationId, OnboardingDocumentCategory.DNI_BACK)
                .orElseThrow(() -> new IllegalStateException("DNI back reference disappeared after submission."));
        OnboardingTermsAcceptance terms = termsRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalStateException("Terms acceptance disappeared after submission."));
        OnboardingReviewPolicyPort.ReviewPolicy policy = reviewPolicy.policy(application.reviewPolicyVersion());
        return new ReviewContext(application, applicant, front, back, terms, policy);
    }

    private List<OnboardingReviewCheck> evaluate(ReviewContext context, Instant now) {
        List<OnboardingReviewCheck> checks = new ArrayList<>();
        boolean emailReserved = reservationPort.tryAcquire(UniquenessReservationType.EMAIL,
                context.application().email().trim().toLowerCase(java.util.Locale.ROOT), context.application().id(), now);
        String normalizedDocument = context.applicant().documentType().name() + "|" + context.applicant().documentIssuingCountry()
                + "|" + context.applicant().documentNumber().replaceAll("[^A-Za-z0-9]", "").toUpperCase(java.util.Locale.ROOT);
        boolean documentReserved = reservationPort.tryAcquire(UniquenessReservationType.DOCUMENT,
                normalizedDocument, context.application().id(), now);
        boolean duplicate = !emailReserved || !documentReserved
                || customerLookup.existsByDocument(context.applicant().documentType(), context.applicant().documentNumber(), context.applicant().documentIssuingCountry())
                || customerLookup.existsByEmail(context.application().email())
                || applicantDataRepository.existsActiveApplicationByDocumentExcluding(context.application().id(), context.applicant().documentType().name(), context.applicant().documentNumber(), context.applicant().documentIssuingCountry())
                || applicationRepository.existsByEmailAndStatusInExcluding(context.application().id(), context.application().email(), ACTIVE_STATUSES);
        checks.add(local(context, ReviewCheckType.DUPLICATE_CHECK, !duplicate, duplicate ? "DUPLICATE_IDENTITY" : "NO_DUPLICATE_FOUND", now));

        boolean documentsValid = documentValidation.isStoredOnboardingDocument(context.front().documentId(), context.application().id(), OnboardingDocumentCategory.DNI_FRONT)
                && documentValidation.isStoredOnboardingDocument(context.back().documentId(), context.application().id(), OnboardingDocumentCategory.DNI_BACK);
        checks.add(local(context, ReviewCheckType.DOCUMENTS_PRESENT, documentsValid, documentsValid ? "REQUIRED_DOCUMENTS_PRESENT" : "DOCUMENT_REFERENCE_INVALID", now));

        boolean termsValid = context.policy().requiredTermsVersion().equals(context.terms().termsVersion());
        checks.add(local(context, ReviewCheckType.TERMS_ACCEPTED, termsValid, termsValid ? "TERMS_VERSION_ACCEPTED" : "TERMS_VERSION_INVALID", now));

        boolean eligible = basicEligibility(context.applicant(), LocalDate.ofInstant(now, reviewPolicy.businessZone()));
        checks.add(local(context, ReviewCheckType.BASIC_ELIGIBILITY, eligible, eligible ? "BASIC_ELIGIBILITY_PASSED" : "BASIC_ELIGIBILITY_FAILED", now));

        checks.add(configuredIntegrationCheck(context, ReviewCheckType.DOCUMENT_PROOFING, now));
        checks.add(configuredIntegrationCheck(context, ReviewCheckType.SANCTIONS_PEP_SCREENING, now));
        checks.add(configuredIntegrationCheck(context, ReviewCheckType.FRAUD_SCREENING, now));
        return checks;
    }

    private boolean basicEligibility(ApplicantData applicant, LocalDate today) {
        return Period.between(applicant.birthDate(), today).getYears() >= 18
                && applicant.documentType() == ApplicantDocumentType.DNI
                && "AR".equals(applicant.documentIssuingCountry())
                && "AR".equals(applicant.country())
                && (applicant.documentExpirationDate() == null || !applicant.documentExpirationDate().isBefore(today));
    }

    private OnboardingReviewCheck local(ReviewContext context, ReviewCheckType type, boolean passed, String reason, Instant now) {
        requireMode(context, type, ReviewCheckExecutionMode.LOCAL);
        return OnboardingReviewCheck.completed(context.application().id(), type, ReviewCheckExecutionMode.LOCAL,
                passed ? ReviewCheckOutcome.PASSED : ReviewCheckOutcome.FAILED, true,
                context.application().reviewPolicyVersion(), "LOCAL_RULES", reason, now);
    }

    private OnboardingReviewCheck simulated(ReviewContext context, ReviewCheckType type, Instant now) {
        return OnboardingReviewCheck.completed(context.application().id(), type, ReviewCheckExecutionMode.SIMULATED,
                ReviewCheckOutcome.PASSED, true, context.application().reviewPolicyVersion(),
                "SIMULATOR", "SIMULATED_APPROVAL", now);
    }

    private OnboardingReviewCheck configuredIntegrationCheck(ReviewContext context, ReviewCheckType type, Instant now) {
        return switch (context.policy().modeFor(type)) {
            case SIMULATED -> simulated(context, type, now);
            case DISABLED -> OnboardingReviewCheck.completed(
                    context.application().id(), type, ReviewCheckExecutionMode.DISABLED,
                    ReviewCheckOutcome.SKIPPED, false, context.application().reviewPolicyVersion(),
                    "CONFIGURATION", "CHECK_DISABLED", now
            );
            case LOCAL, EXTERNAL -> throw new IllegalStateException(
                    "No review strategy is available for " + type + " in mode " + context.policy().modeFor(type)
            );
        };
    }

    private void requireMode(ReviewContext context, ReviewCheckType type, ReviewCheckExecutionMode expected) {
        if (context.policy().modeFor(type) != expected) {
            throw new IllegalStateException(
                    "No review strategy is available for " + type + " in mode "
                            + context.policy().modeFor(type)
            );
        }
    }

    private void finishReview(OnboardingWorkItem workItem, OnboardingApplication startedApplication,
                              List<OnboardingReviewCheck> checks, boolean rejected, Instant now) {
        checks.forEach(checkRepository::save);
        OnboardingApplication current = requireApplication(startedApplication.id());
        OnboardingApplication decided = applicationRepository.save(rejected ? current.reject(now) : current.approve(now));
        saveHistory(current, decided, rejected ? "AUTO_REVIEW_REJECTED" : "AUTO_REVIEW_APPROVED", OnboardingActorType.AUTO_REVIEW, now);
        if (!rejected) {
            workItemRepository.findByApplicationIdAndJobType(current.id(), WorkflowJobType.PROVISIONING)
                    .orElseGet(() -> workItemRepository.save(OnboardingWorkItem.pending(current.id(), WorkflowJobType.PROVISIONING, now)));
        }
        if (rejected) {
            reservationPort.releaseByApplicationId(current.id(), now);
        }
        workItemRepository.save(workItem.succeed(now));
    }

    private OnboardingApplication requireApplication(java.util.UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(applicationId));
    }

    private void saveHistory(OnboardingApplication previous, OnboardingApplication next, String reason,
                             OnboardingActorType actor, Instant now) {
        historyRepository.save(OnboardingStatusHistory.transition(previous.id(), previous.status(), next.status(), reason, actor, now));
    }

    private record ReviewContext(
            OnboardingApplication application,
            ApplicantData applicant,
            OnboardingDocumentReference front,
            OnboardingDocumentReference back,
            OnboardingTermsAcceptance terms,
            OnboardingReviewPolicyPort.ReviewPolicy policy
    ) {}
}
