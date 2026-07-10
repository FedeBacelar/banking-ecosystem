package com.fedebacelar.bank.onboarding.application.port.out;

import java.util.UUID;

public interface IdentityProvisioningPort {
    UUID createOrResolve(UUID customerId, String keycloakSubject);
    boolean isActive(String keycloakSubject, UUID customerId);
}
