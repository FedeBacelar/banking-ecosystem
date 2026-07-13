package com.fedebacelar.bank.onboarding.application.port.in;

import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;

public interface ResendCredentialInvitationUseCase {
    OnboardingSubmissionDetails resend(String continuationToken, String idempotencyKey);
}
