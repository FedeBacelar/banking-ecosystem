package com.fedebacelar.bank.onboarding.application.port.out;

import java.time.Duration;

public interface OnboardingProvisioningPolicyPort {

    int maxAttempts();

    Duration workerLease();

    int workerBatchSize();

    Duration retryDelay(int attempt);

    Duration credentialReconciliationDelay();

    Duration credentialSetupTimeout();
}
