package com.fedebacelar.bank.homebanking.bff.application.usecase;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingCompletionStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionStatus;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubjectStatus;
import org.springframework.stereotype.Service;

@Service
public class GetOnboardingCompletionStatusService implements GetOnboardingCompletionStatusUseCase {

    private final OnboardingServicePort onboardingServicePort;
    private final GetInternalAccessTokenPort getInternalAccessTokenPort;

    public GetOnboardingCompletionStatusService(
            OnboardingServicePort onboardingServicePort,
            GetInternalAccessTokenPort getInternalAccessTokenPort
    ) {
        this.onboardingServicePort = onboardingServicePort;
        this.getInternalAccessTokenPort = getInternalAccessTokenPort;
    }

    @Override
    public OnboardingCompletionStatus getForKeycloakSubject(String keycloakSubject) {
        OnboardingSubjectStatus source = onboardingServicePort.getCompletionStatus(
                keycloakSubject,
                getInternalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)
        );
        return new OnboardingCompletionStatus(toPublicState(source.status()), source.updatedAt());
    }

    private OnboardingCompletionState toPublicState(OnboardingState state) {
        return switch (state) {
            case COMPLETED -> OnboardingCompletionState.COMPLETED;
            case APPROVED, PROVISIONING, CREDENTIAL_SETUP_PENDING -> OnboardingCompletionState.PROCESSING;
            case EMAIL_VERIFICATION_PENDING, IN_PROGRESS, SUBMITTED, UNDER_AUTOMATED_REVIEW,
                    REVIEW_FAILED, PROVISIONING_FAILED, CREDENTIAL_SETUP_EXPIRED,
                    CREDENTIAL_SETUP_FAILED, REJECTED, EXPIRED, CANCELLED -> OnboardingCompletionState.FAILED;
        };
    }
}
