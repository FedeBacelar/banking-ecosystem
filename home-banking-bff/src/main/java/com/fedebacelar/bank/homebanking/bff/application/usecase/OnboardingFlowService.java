package com.fedebacelar.bank.homebanking.bff.application.usecase;

import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.homebanking.bff.application.port.in.AcceptOnboardingTermsUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingSessionUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.SaveOnboardingApplicantDataUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.UploadOnboardingDocumentUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.out.DocumentServicePort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocument;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OnboardingFlowService implements
        StartOnboardingApplicationUseCase,
        ConsumeOnboardingMagicLinkUseCase,
        SaveOnboardingApplicantDataUseCase,
        UploadOnboardingDocumentUseCase,
        AcceptOnboardingTermsUseCase,
        GetOnboardingSessionUseCase,
        SubmitOnboardingUseCase,
        GetOnboardingStatusUseCase,
        ResendCredentialInvitationUseCase {

    private static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_DOCUMENT_CATEGORIES = Set.of("DNI_FRONT", "DNI_BACK");
    private static final Set<String> ALLOWED_DOCUMENT_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

    private final OnboardingServicePort onboardingServicePort;
    private final DocumentServicePort documentServicePort;
    private final GetInternalAccessTokenPort getInternalAccessTokenPort;

    public OnboardingFlowService(
            OnboardingServicePort onboardingServicePort,
            DocumentServicePort documentServicePort,
            GetInternalAccessTokenPort getInternalAccessTokenPort
    ) {
        this.onboardingServicePort = onboardingServicePort;
        this.documentServicePort = documentServicePort;
        this.getInternalAccessTokenPort = getInternalAccessTokenPort;
    }

    @Override
    public OnboardingApplication startApplication(String email) {
        return onboardingServicePort.startApplication(email, getInternalAccessTokenPort.getAccessToken());
    }

    @Override
    public OnboardingContinuation consumeMagicLink(String token) {
        return onboardingServicePort.consumeMagicLink(token, getInternalAccessTokenPort.getAccessToken());
    }

    @Override
    public OnboardingSession getSession(String continuationToken) {
        if (!StringUtils.hasText(continuationToken)) {
            return OnboardingSession.anonymous();
        }

        return onboardingServicePort.validateContinuation(continuationToken, getInternalAccessTokenPort.getAccessToken());
    }

    @Override
    public OnboardingApplicantData saveApplicantData(String continuationToken, OnboardingApplicantData applicantData) {
        if (!StringUtils.hasText(continuationToken)) {
            throw new OnboardingSessionRequiredException();
        }

        return onboardingServicePort.saveApplicantData(continuationToken, applicantData, getInternalAccessTokenPort.getAccessToken());
    }

    @Override
    public OnboardingDocumentReference uploadDocument(String continuationToken, String category, MultipartFile file) {
        if (!StringUtils.hasText(continuationToken)) {
            throw new OnboardingSessionRequiredException();
        }
        validateDocument(category, file);

        String accessToken = getInternalAccessTokenPort.getAccessToken();
        OnboardingSession session = onboardingServicePort.validateContinuation(continuationToken, accessToken);
        OnboardingDocument document = documentServicePort.uploadOnboardingDocument(
                session.applicationId(),
                category,
                file,
                accessToken
        );
        return onboardingServicePort.saveDocumentReference(continuationToken, category, document.id(), accessToken);
    }

    @Override
    public OnboardingTermsAcceptance acceptTerms(String continuationToken, boolean accepted, String termsVersion) {
        if (!StringUtils.hasText(continuationToken)) {
            throw new OnboardingSessionRequiredException();
        }

        return onboardingServicePort.acceptTerms(
                continuationToken,
                accepted,
                termsVersion,
                getInternalAccessTokenPort.getAccessToken()
        );
    }

    @Override
    public OnboardingSubmission submit(String continuationToken) {
        requireSessionToken(continuationToken);
        return onboardingServicePort.submit(continuationToken, getInternalAccessTokenPort.getAccessToken());
    }

    @Override
    public OnboardingPublicStatus getStatus(String continuationToken) {
        requireSessionToken(continuationToken);
        OnboardingSession session = onboardingServicePort.validateContinuation(continuationToken, getInternalAccessTokenPort.getAccessToken());
        return new OnboardingPublicStatus(session.applicationId(), session.status(), nextAction(session.status()), session.updatedAt());
    }

    @Override
    public OnboardingSubmission resendCredentialInvitation(String continuationToken) {
        requireSessionToken(continuationToken);
        return onboardingServicePort.resendCredentialInvitation(continuationToken, getInternalAccessTokenPort.getAccessToken());
    }

    private void requireSessionToken(String continuationToken) {
        if (!StringUtils.hasText(continuationToken)) {
            throw new OnboardingSessionRequiredException();
        }
    }

    private String nextAction(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "CONTINUE_APPLICATION";
            case "SUBMITTED", "UNDER_AUTOMATED_REVIEW", "APPROVED", "PROVISIONING" -> "WAIT";
            case "CREDENTIAL_SETUP_PENDING" -> "CHECK_EMAIL";
            case "COMPLETED" -> "LOGIN";
            case "REVIEW_FAILED", "PROVISIONING_FAILED" -> "CONTACT_SUPPORT";
            case "EXPIRED", "CANCELLED" -> "START_NEW_APPLICATION";
            default -> "NONE";
        };
    }

    private void validateDocument(String category, MultipartFile file) {
        if (!StringUtils.hasText(category) || !ALLOWED_DOCUMENT_CATEGORIES.contains(category)) {
            throw new InvalidOnboardingDocumentException("Unsupported onboarding document category.");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidOnboardingDocumentException("Document file is required.");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new InvalidOnboardingDocumentException("Document file is too large.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_DOCUMENT_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidOnboardingDocumentException("Unsupported document file type.");
        }
    }
}
