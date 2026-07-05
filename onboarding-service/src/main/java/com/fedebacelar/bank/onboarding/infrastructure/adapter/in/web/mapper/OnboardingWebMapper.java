package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationRequest;
import org.springframework.stereotype.Component;

@Component
public class OnboardingWebMapper {

    public StartOnboardingApplicationCommand toCommand(StartOnboardingApplicationRequest request) {
        return new StartOnboardingApplicationCommand(request.email());
    }

    public ConsumeMagicLinkCommand toCommand(ConsumeMagicLinkRequest request) {
        return new ConsumeMagicLinkCommand(request.token());
    }

    public ValidateContinuationCommand toCommand(ValidateContinuationRequest request) {
        return new ValidateContinuationCommand(request.token());
    }

    public OnboardingApplicationResponse toResponse(OnboardingApplicationDetails details) {
        return new OnboardingApplicationResponse(
                details.id(),
                details.email(),
                details.status(),
                details.magicLinkExpiresAt(),
                details.magicLinkConsumedAt(),
                details.emailVerifiedAt(),
                details.continuationExpiresAt(),
                details.expiresAt(),
                details.createdAt(),
                details.updatedAt()
        );
    }

    public OnboardingContinuationResponse toResponse(OnboardingContinuationDetails details) {
        return new OnboardingContinuationResponse(
                details.applicationId(),
                details.email(),
                details.status(),
                details.continuationToken(),
                details.continuationExpiresAt()
        );
    }
}
