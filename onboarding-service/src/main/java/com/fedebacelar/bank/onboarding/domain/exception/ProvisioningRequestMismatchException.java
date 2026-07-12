package com.fedebacelar.bank.onboarding.domain.exception;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;

public class ProvisioningRequestMismatchException extends RuntimeException {
    public ProvisioningRequestMismatchException(ProvisioningStepType stepType) {
        super("Provisioning request changed for an existing step: " + stepType);
    }
}
