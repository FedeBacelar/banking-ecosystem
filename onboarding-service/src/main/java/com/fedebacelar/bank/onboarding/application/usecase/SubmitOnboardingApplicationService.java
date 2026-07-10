package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingIncompleteException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitOnboardingApplicationService implements SubmitOnboardingUseCase {
    private static final Set<OnboardingApplicationStatus> ALREADY_SUBMITTED = Set.of(
            OnboardingApplicationStatus.SUBMITTED,
            OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW,
            OnboardingApplicationStatus.REVIEW_FAILED,
            OnboardingApplicationStatus.APPROVED,
            OnboardingApplicationStatus.REJECTED,
            OnboardingApplicationStatus.PROVISIONING,
            OnboardingApplicationStatus.PROVISIONING_FAILED,
            OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING,
            OnboardingApplicationStatus.COMPLETED
    );

    private final OnboardingApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicantDataRepositoryPort applicantDataRepository;
    private final OnboardingDocumentReferenceRepositoryPort documentRepository;
    private final OnboardingTermsAcceptanceRepositoryPort termsRepository;
    private final OnboardingStatusHistoryRepositoryPort statusHistoryRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final TokenHashingPort tokenHashingPort;
    private final Clock clock;
    private final String requiredTermsVersion;

    public SubmitOnboardingApplicationService(
            OnboardingApplicationRepositoryPort applicationRepository,
            OnboardingApplicantDataRepositoryPort applicantDataRepository,
            OnboardingDocumentReferenceRepositoryPort documentRepository,
            OnboardingTermsAcceptanceRepositoryPort termsRepository,
            OnboardingStatusHistoryRepositoryPort statusHistoryRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            TokenHashingPort tokenHashingPort,
            Clock clock,
            @Value("${onboarding.review.required-terms-version:ONBOARDING_TERMS_AR_V1}") String requiredTermsVersion
    ) {
        this.applicationRepository = applicationRepository;
        this.applicantDataRepository = applicantDataRepository;
        this.documentRepository = documentRepository;
        this.termsRepository = termsRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.workItemRepository = workItemRepository;
        this.tokenHashingPort = tokenHashingPort;
        this.clock = clock;
        this.requiredTermsVersion = requiredTermsVersion;
    }

    @Override
    @Transactional
    public OnboardingSubmissionDetails submit(SubmitOnboardingCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applicationRepository
                .findByContinuationTokenHash(tokenHashingPort.hash(command.continuationToken()))
                .orElseThrow(InvalidContinuationTokenException::new);

        if (application.continuationExpired(now)) {
            throw new OnboardingContinuationExpiredException();
        }
        if (ALREADY_SUBMITTED.contains(application.status())) {
            return details(application);
        }
        if (application.status() != OnboardingApplicationStatus.IN_PROGRESS) {
            throw new InvalidOnboardingStatusTransitionException(application.status(), OnboardingApplicationStatus.SUBMITTED);
        }

        Set<String> missingSections = missingSections(application);
        if (!missingSections.isEmpty()) {
            throw new OnboardingIncompleteException(missingSections);
        }

        OnboardingApplication submitted = applicationRepository.save(application.submit(now));
        statusHistoryRepository.save(OnboardingStatusHistory.transition(
                application.id(), application.status(), submitted.status(), "APPLICATION_SUBMITTED",
                OnboardingActorType.APPLICANT_SESSION, now
        ));
        workItemRepository.findByApplicationIdAndJobType(application.id(), WorkflowJobType.AUTO_REVIEW)
                .orElseGet(() -> workItemRepository.save(OnboardingWorkItem.pending(application.id(), WorkflowJobType.AUTO_REVIEW, now)));
        return details(submitted);
    }

    private Set<String> missingSections(OnboardingApplication application) {
        Set<String> missing = new LinkedHashSet<>();
        if (applicantDataRepository.findByApplicationId(application.id()).isEmpty()) {
            missing.add("APPLICANT_DATA");
        }
        if (documentRepository.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_FRONT).isEmpty()) {
            missing.add("DNI_FRONT");
        }
        if (documentRepository.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_BACK).isEmpty()) {
            missing.add("DNI_BACK");
        }
        if (termsRepository.findByApplicationId(application.id())
                .filter(terms -> requiredTermsVersion.equals(terms.termsVersion()))
                .isEmpty()) {
            missing.add("TERMS");
        }
        return missing;
    }

    private OnboardingSubmissionDetails details(OnboardingApplication application) {
        return new OnboardingSubmissionDetails(application.id(), application.status(), application.submittedAt(), application.updatedAt());
    }
}
