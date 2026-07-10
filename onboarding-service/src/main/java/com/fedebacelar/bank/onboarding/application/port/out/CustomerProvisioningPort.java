package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import java.util.UUID;

public interface CustomerProvisioningPort {
    UUID createCustomer(UUID applicationId, String email, ApplicantData applicantData);
    void approveKyc(UUID customerId);
    boolean isActive(UUID customerId);
}
