package com.fedebacelar.bank.onboarding.application.port.out;

import java.util.UUID;

public interface AccountProvisioningPort {
    UUID openDefaultAccount(UUID applicationId, UUID customerId);
    void activate(UUID accountId);
    boolean isActive(UUID accountId);
}
