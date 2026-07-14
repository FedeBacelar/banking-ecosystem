CREATE UNIQUE INDEX uk_onboarding_provisioning_step_reference
    ON onboarding_provisioning_step (step_type, external_reference);
