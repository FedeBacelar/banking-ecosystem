package com.fedebacelar.bank.homebanking.bff.application.usecase;

import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingSessionUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OnboardingFlowService implements
        StartOnboardingApplicationUseCase,
        ConsumeOnboardingMagicLinkUseCase,
        GetOnboardingSessionUseCase {

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
}
