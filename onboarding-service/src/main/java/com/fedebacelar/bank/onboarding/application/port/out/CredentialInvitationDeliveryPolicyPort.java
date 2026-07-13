package com.fedebacelar.bank.onboarding.application.port.out;

import java.time.Duration;

public interface CredentialInvitationDeliveryPolicyPort {
    Duration credentialInvitationCooldown();

    int maxAttempts();

    int workerBatchSize();

    Duration workerLease();

    Duration retryDelay(int attempt);
}
