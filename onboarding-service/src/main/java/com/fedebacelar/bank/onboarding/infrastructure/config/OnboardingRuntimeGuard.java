package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OnboardingRuntimeGuard {

    private static final String LOCAL_ENCRYPTION_KEY =
            "bG9jYWwtb25ib2FyZGluZy1wYXlsb2FkLWtleS0wMDE=";

    private final String runtimeMode;
    private final String encryptionKey;
    private final OnboardingReviewProperties reviewProperties;

    public OnboardingRuntimeGuard(
            @Value("${onboarding.runtime-mode:LOCAL}") String runtimeMode,
            @Value("${onboarding.security.payload-encryption-key}") String encryptionKey,
            OnboardingReviewProperties reviewProperties
    ) {
        this.runtimeMode = runtimeMode;
        this.encryptionKey = encryptionKey;
        this.reviewProperties = reviewProperties;
    }

    @PostConstruct
    void validate() {
        if ("LOCAL".equalsIgnoreCase(runtimeMode)) {
            return;
        }
        boolean simulated = reviewProperties.getPolicies().values().stream()
                .flatMap(policy -> policy.getChecks().values().stream())
                .anyMatch(mode -> mode == ReviewCheckExecutionMode.SIMULATED);
        if (simulated) {
            throw new IllegalStateException("SIMULATED onboarding checks are allowed only in LOCAL runtime mode.");
        }
        if (LOCAL_ENCRYPTION_KEY.equals(encryptionKey)) {
            throw new IllegalStateException("The local onboarding encryption key cannot be used outside LOCAL mode.");
        }
    }
}
