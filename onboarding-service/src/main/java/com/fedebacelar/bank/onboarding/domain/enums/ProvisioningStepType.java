package com.fedebacelar.bank.onboarding.domain.enums;

public enum ProvisioningStepType {
    CREATE_CUSTOMER,
    APPROVE_CUSTOMER_KYC,
    OPEN_ACCOUNT,
    PRECREATE_KEYCLOAK_USER,
    CREATE_IDENTITY_LINK,
    ACTIVATE_ACCOUNT,
    SEND_CREDENTIAL_SETUP_EMAIL
}
