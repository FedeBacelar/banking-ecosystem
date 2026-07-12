package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentUploadPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentUploadException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class SubmitOnboardingApplicationService implements SubmitOnboardingUseCase {

    private final OnboardingApplicationRepositoryPort applicationRepository;
    private final OnboardingDocumentUploadPort documentUploadPort;
    private final OnboardingSubmissionFinalizer submissionFinalizer;
    private final TokenHashingPort tokenHashingPort;
    private final Clock clock;

    public SubmitOnboardingApplicationService(
            OnboardingApplicationRepositoryPort applicationRepository,
            OnboardingDocumentUploadPort documentUploadPort,
            OnboardingSubmissionFinalizer submissionFinalizer,
            TokenHashingPort tokenHashingPort,
            Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.documentUploadPort = documentUploadPort;
        this.submissionFinalizer = submissionFinalizer;
        this.tokenHashingPort = tokenHashingPort;
        this.clock = clock;
    }

    @Override
    public OnboardingSubmissionDetails submit(SubmitOnboardingCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = applicationRepository
                .findByContinuationTokenHash(tokenHashingPort.hash(command.continuationToken()))
                .orElseThrow(InvalidContinuationTokenException::new);

        validateAccess(application, now);
        if (application.hasBeenSubmitted()) {
            return details(application);
        }
        validateNewSubmission(application, command);

        String frontHash = sha256(command.dniFront());
        String backHash = sha256(command.dniBack());
        var frontId = documentUploadPort.upload(
                application.id(), OnboardingDocumentCategory.DNI_FRONT, command.dniFront(), frontHash
        );
        var backId = documentUploadPort.upload(
                application.id(), OnboardingDocumentCategory.DNI_BACK, command.dniBack(), backHash
        );

        return submissionFinalizer.complete(command, frontId, backId);
    }

    private void validateAccess(OnboardingApplication application, Instant now) {
        if (application.continuationExpired(now) || !now.isBefore(application.expiresAt())) {
            throw new OnboardingContinuationExpiredException();
        }
    }

    private void validateNewSubmission(OnboardingApplication application, SubmitOnboardingCommand command) {
        if (application.status() != OnboardingApplicationStatus.IN_PROGRESS) {
            throw new InvalidOnboardingStatusTransitionException(
                    application.status(), OnboardingApplicationStatus.SUBMITTED
            );
        }
        if (!command.termsAccepted()) {
            throw new TermsAcceptanceRequiredException();
        }
    }

    private String sha256(OnboardingDocumentUpload document) {
        try (InputStream input = document.openStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new OnboardingDocumentUploadException("Could not hash onboarding document.", exception);
        }
    }

    private OnboardingSubmissionDetails details(OnboardingApplication application) {
        return new OnboardingSubmissionDetails(
                application.id(), application.status(), application.submittedAt(), application.updatedAt()
        );
    }
}
