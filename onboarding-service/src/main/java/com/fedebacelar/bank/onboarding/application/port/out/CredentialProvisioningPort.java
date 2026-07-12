package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.application.model.CredentialSetupState;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import java.util.UUID;

public interface CredentialProvisioningPort {
    String precreateUser(UUID applicationId, String email, ApplicantData applicantData);
    void sendCredentialSetupEmail(String userId);
    CredentialSetupState getCredentialSetupState(String userId);
}
