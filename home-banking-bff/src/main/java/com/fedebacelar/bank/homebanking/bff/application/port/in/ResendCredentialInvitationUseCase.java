package com.fedebacelar.bank.homebanking.bff.application.port.in;

import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;

public interface ResendCredentialInvitationUseCase {
    OnboardingSubmission resendCredentialInvitation(String continuationToken, String idempotencyKey);
}
