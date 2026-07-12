package com.fedebacelar.bank.onboarding.application.port.out;

public interface ProvisioningFailureClassifierPort {

    boolean isRetryable(RuntimeException exception);
}
