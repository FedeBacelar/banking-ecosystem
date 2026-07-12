package com.fedebacelar.bank.homebanking.bff.application.usecase;

import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OnboardingFlowService implements
        StartOnboardingApplicationUseCase,
        ConsumeOnboardingMagicLinkUseCase,
        SubmitOnboardingUseCase,
        GetOnboardingStatusUseCase,
        ResendCredentialInvitationUseCase {

    private final OnboardingServicePort onboardingServicePort;
    private final GetInternalAccessTokenPort getInternalAccessTokenPort;

    public OnboardingFlowService(
            OnboardingServicePort onboardingServicePort,
            GetInternalAccessTokenPort getInternalAccessTokenPort
    ) {
        this.onboardingServicePort = onboardingServicePort;
        this.getInternalAccessTokenPort = getInternalAccessTokenPort;
    }

    @Override
    public void startApplication(String email) {
        onboardingServicePort.startApplication(email, onboardingAccessToken());
    }

    @Override
    public OnboardingContinuation consumeMagicLink(String token) {
        return onboardingServicePort.consumeMagicLink(token, onboardingAccessToken());
    }

    @Override
    public OnboardingSubmission submit(
            String continuationToken,
            OnboardingApplicantData applicantData,
            boolean termsAccepted,
            OnboardingFile dniFront,
            OnboardingFile dniBack
    ) {
        requireContinuation(continuationToken);
        return onboardingServicePort.submit(
                continuationToken,
                applicantData,
                termsAccepted,
                dniFront,
                dniBack,
                onboardingAccessToken()
        );
    }

    @Override
    public OnboardingPublicStatus getStatus(String continuationToken) {
        requireContinuation(continuationToken);
        OnboardingSession session = onboardingServicePort.validateContinuation(
                continuationToken,
                onboardingAccessToken()
        );
        return new OnboardingPublicStatus(
                session.applicationId(),
                session.status(),
                session.status().nextAction(),
                session.updatedAt()
        );
    }

    @Override
    public OnboardingSubmission resendCredentialInvitation(String continuationToken) {
        requireContinuation(continuationToken);
        return onboardingServicePort.resendCredentialInvitation(
                continuationToken,
                onboardingAccessToken()
        );
    }

    private void requireContinuation(String continuationToken) {
        if (!StringUtils.hasText(continuationToken)) {
            throw new OnboardingSessionRequiredException();
        }
    }

    private String onboardingAccessToken() {
        return getInternalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING);
    }

}
