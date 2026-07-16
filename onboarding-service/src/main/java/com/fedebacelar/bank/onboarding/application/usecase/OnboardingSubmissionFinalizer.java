package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OnboardingSubmissionFinalizer {

    private final OnboardingApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicantDataRepositoryPort applicantDataRepository;
    private final OnboardingDocumentReferenceRepositoryPort documentRepository;
    private final OnboardingTermsAcceptanceRepositoryPort termsRepository;
    private final OnboardingStatusHistoryRepositoryPort statusHistoryRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final TokenHashingPort tokenHashingPort;
    private final OnboardingReviewPolicyPort reviewPolicy;
    private final OnboardingTelemetryPort telemetry;
    private final Clock clock;

    public OnboardingSubmissionFinalizer(
            OnboardingApplicationRepositoryPort applicationRepository,
            OnboardingApplicantDataRepositoryPort applicantDataRepository,
            OnboardingDocumentReferenceRepositoryPort documentRepository,
            OnboardingTermsAcceptanceRepositoryPort termsRepository,
            OnboardingStatusHistoryRepositoryPort statusHistoryRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            TokenHashingPort tokenHashingPort,
            OnboardingReviewPolicyPort reviewPolicy,
            OnboardingTelemetryPort telemetry,
            Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.applicantDataRepository = applicantDataRepository;
        this.documentRepository = documentRepository;
        this.termsRepository = termsRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.workItemRepository = workItemRepository;
        this.tokenHashingPort = tokenHashingPort;
        this.reviewPolicy = reviewPolicy;
        this.telemetry = telemetry;
        this.clock = clock;
    }

    @Transactional
    public OnboardingSubmissionDetails complete(
            SubmitOnboardingCommand command,
            UUID dniFrontId,
            UUID dniBackId
    ) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applicationRepository
                .findByContinuationTokenHashForUpdate(tokenHashingPort.hash(command.continuationToken()))
                .orElseThrow(InvalidContinuationTokenException::new);

        if (application.continuationExpired(now) || !now.isBefore(application.expiresAt())) {
            throw new OnboardingContinuationExpiredException();
        }
        if (application.hasBeenSubmitted()) {
            return details(application);
        }
        if (application.status() != OnboardingApplicationStatus.IN_PROGRESS) {
            throw new InvalidOnboardingStatusTransitionException(
                    application.status(), OnboardingApplicationStatus.SUBMITTED
            );
        }
        if (!command.termsAccepted()) {
            throw new TermsAcceptanceRequiredException();
        }
        reviewPolicy.policy(application.reviewPolicyVersion());

        saveApplicantData(application.id(), command, now);
        saveDocumentReference(application.id(), OnboardingDocumentCategory.DNI_FRONT, dniFrontId, now);
        saveDocumentReference(application.id(), OnboardingDocumentCategory.DNI_BACK, dniBackId, now);
        saveTermsAcceptance(application.id(), application.reviewPolicyVersion(), now);

        OnboardingApplication submitted = applicationRepository.save(application.submit(now));
        statusHistoryRepository.save(OnboardingStatusHistory.transition(
                application.id(), application.status(), submitted.status(), "APPLICATION_SUBMITTED",
                OnboardingActorType.APPLICANT_SESSION, now
        ));
        workItemRepository.findByApplicationIdAndJobType(application.id(), WorkflowJobType.AUTO_REVIEW)
                .orElseGet(() -> workItemRepository.save(
                        OnboardingWorkItem.pending(application.id(), WorkflowJobType.AUTO_REVIEW, now)
                ));
        telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.SUBMITTED);
        return details(submitted);
    }

    private void saveApplicantData(UUID applicationId, SubmitOnboardingCommand command, Instant now) {
        ApplicantData incoming = ApplicantData.create(
                applicationId,
                required(command.firstName()),
                optional(command.middleName()),
                required(command.lastName()),
                command.birthDate(),
                country(command.nationality()),
                command.documentType(),
                required(command.documentNumber()),
                country(command.documentIssuingCountry()),
                command.documentExpirationDate(),
                required(command.phoneNumber()),
                required(command.street()),
                required(command.streetNumber()),
                required(command.city()),
                required(command.province()),
                required(command.postalCode()),
                country(command.country()),
                now
        );
        applicantDataRepository.findByApplicationId(applicationId)
                .map(existing -> existing.updateFrom(incoming, now))
                .map(applicantDataRepository::save)
                .orElseGet(() -> applicantDataRepository.save(incoming));
    }

    private void saveDocumentReference(
            UUID applicationId,
            OnboardingDocumentCategory category,
            UUID documentId,
            Instant now
    ) {
        documentRepository.findByApplicationIdAndCategory(applicationId, category)
                .map(existing -> existing.updateDocument(documentId, now))
                .map(documentRepository::save)
                .orElseGet(() -> documentRepository.save(
                        OnboardingDocumentReference.create(applicationId, category, documentId, now)
                ));
    }

    private void saveTermsAcceptance(UUID applicationId, String policyVersion, Instant now) {
        String requiredVersion = reviewPolicy.policy(policyVersion).requiredTermsVersion();
        termsRepository.findByApplicationId(applicationId).ifPresentOrElse(existing -> {
            if (!requiredVersion.equals(existing.termsVersion())) {
                throw new TermsAcceptanceRequiredException();
            }
        }, () -> termsRepository.save(OnboardingTermsAcceptance.accept(applicationId, requiredVersion, now)));
    }

    private String required(String value) {
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String country(String value) {
        return required(value).toUpperCase(Locale.ROOT);
    }

    private OnboardingSubmissionDetails details(OnboardingApplication application) {
        return new OnboardingSubmissionDetails(
                application.id(), application.status(), application.submittedAt(), application.updatedAt()
        );
    }
}
