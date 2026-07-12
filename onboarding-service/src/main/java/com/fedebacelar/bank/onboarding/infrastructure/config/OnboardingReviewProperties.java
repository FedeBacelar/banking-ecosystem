package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingReviewMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.review")
public class OnboardingReviewProperties implements OnboardingReviewPolicyPort {
    private OnboardingReviewMode mode = OnboardingReviewMode.AUTO;
    private String activePolicyVersion = "AR_DNI_SAVINGS_V1";
    private int maxAttempts = 6;
    private Duration workerLease = Duration.ofMinutes(2);
    private int workerBatchSize = 20;
    private ZoneId businessZone = ZoneId.of("America/Argentina/Buenos_Aires");
    private List<Duration> retryBackoff = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofMinutes(2),
            Duration.ofMinutes(10), Duration.ofMinutes(30)
    );
    private Map<String, PolicyDefinition> policies = defaultPolicies();

    @PostConstruct
    void validate() {
        if (mode != OnboardingReviewMode.AUTO) {
            throw new IllegalStateException("MANUAL onboarding review is not implemented.");
        }
        if (!policies.containsKey(activePolicyVersion)) {
            throw new IllegalStateException("Active onboarding review policy is not configured: " + activePolicyVersion);
        }
        policies.forEach(this::validatePolicy);
    }

    public PolicyDefinition policy(String version) {
        PolicyDefinition policy = policies.get(version);
        if (policy == null) {
            throw new IllegalStateException("Onboarding review policy is not available: " + version);
        }
        return policy;
    }

    public Duration retryDelay(int attempt) {
        int index = Math.max(0, Math.min(attempt - 1, retryBackoff.size() - 1));
        return retryBackoff.get(index);
    }

    private void validatePolicy(String version, PolicyDefinition policy) {
        if (policy.requiredTermsVersion == null || policy.requiredTermsVersion.isBlank()) {
            throw new IllegalStateException("Missing terms version for onboarding policy " + version);
        }
        for (ReviewCheckType type : ReviewCheckType.values()) {
            if (!policy.checks.containsKey(type)) {
                throw new IllegalStateException("Missing " + type + " mode for onboarding policy " + version);
            }
        }
        List<ReviewCheckType> localChecks = List.of(
                ReviewCheckType.DUPLICATE_CHECK,
                ReviewCheckType.DOCUMENTS_PRESENT,
                ReviewCheckType.TERMS_ACCEPTED,
                ReviewCheckType.BASIC_ELIGIBILITY
        );
        localChecks.forEach(type -> {
            if (policy.checks.get(type) != ReviewCheckExecutionMode.LOCAL) {
                throw new IllegalStateException(type + " must use LOCAL mode in policy " + version);
            }
        });
        policy.checks.forEach((type, executionMode) -> {
            if (executionMode == ReviewCheckExecutionMode.EXTERNAL) {
                throw new IllegalStateException("EXTERNAL strategy is not implemented for " + type);
            }
        });
    }

    private static Map<String, PolicyDefinition> defaultPolicies() {
        Map<String, PolicyDefinition> defaults = new LinkedHashMap<>();
        defaults.put("AR_DNI_SAVINGS_V1", new PolicyDefinition(
                "ONBOARDING_TERMS_AR_V1", defaultChecks()
        ));
        return defaults;
    }

    private static Map<ReviewCheckType, ReviewCheckExecutionMode> defaultChecks() {
        Map<ReviewCheckType, ReviewCheckExecutionMode> modes = new EnumMap<>(ReviewCheckType.class);
        modes.put(ReviewCheckType.DUPLICATE_CHECK, ReviewCheckExecutionMode.LOCAL);
        modes.put(ReviewCheckType.DOCUMENTS_PRESENT, ReviewCheckExecutionMode.LOCAL);
        modes.put(ReviewCheckType.TERMS_ACCEPTED, ReviewCheckExecutionMode.LOCAL);
        modes.put(ReviewCheckType.BASIC_ELIGIBILITY, ReviewCheckExecutionMode.LOCAL);
        modes.put(ReviewCheckType.DOCUMENT_PROOFING, ReviewCheckExecutionMode.SIMULATED);
        modes.put(ReviewCheckType.SANCTIONS_PEP_SCREENING, ReviewCheckExecutionMode.SIMULATED);
        modes.put(ReviewCheckType.FRAUD_SCREENING, ReviewCheckExecutionMode.SIMULATED);
        return modes;
    }

    public OnboardingReviewMode getMode() { return mode; }
    public void setMode(OnboardingReviewMode mode) { this.mode = mode; }
    public String getPolicyVersion() { return activePolicyVersion; }
    public void setPolicyVersion(String policyVersion) { this.activePolicyVersion = policyVersion; }
    public String getActivePolicyVersion() { return activePolicyVersion; }
    public void setActivePolicyVersion(String activePolicyVersion) { this.activePolicyVersion = activePolicyVersion; }
    public String getRequiredTermsVersion() { return policy(activePolicyVersion).requiredTermsVersion; }
    public void setRequiredTermsVersion(String requiredTermsVersion) {
        policy(activePolicyVersion).requiredTermsVersion = requiredTermsVersion;
    }
    public Map<ReviewCheckType, ReviewCheckExecutionMode> getChecks() {
        return policy(activePolicyVersion).checks;
    }
    public void setChecks(Map<ReviewCheckType, ReviewCheckExecutionMode> checks) {
        policy(activePolicyVersion).checks = checks;
    }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getWorkerLease() { return workerLease; }
    public void setWorkerLease(Duration workerLease) { this.workerLease = workerLease; }
    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int workerBatchSize) { this.workerBatchSize = workerBatchSize; }
    public ZoneId getBusinessZone() { return businessZone; }
    public void setBusinessZone(ZoneId businessZone) { this.businessZone = businessZone; }
    public List<Duration> getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(List<Duration> retryBackoff) { this.retryBackoff = retryBackoff; }
    public Map<String, PolicyDefinition> getPolicies() { return policies; }
    public void setPolicies(Map<String, PolicyDefinition> policies) { this.policies = policies; }

    @Override public OnboardingReviewMode mode() { return mode; }
    @Override public String activePolicyVersion() { return activePolicyVersion; }
    @Override public int maxAttempts() { return maxAttempts; }
    @Override public Duration workerLease() { return workerLease; }
    @Override public int workerBatchSize() { return workerBatchSize; }
    @Override public ZoneId businessZone() { return businessZone; }

    public static class PolicyDefinition implements OnboardingReviewPolicyPort.ReviewPolicy {
        private String requiredTermsVersion;
        private Map<ReviewCheckType, ReviewCheckExecutionMode> checks = new EnumMap<>(ReviewCheckType.class);

        public PolicyDefinition() {
        }

        PolicyDefinition(
                String requiredTermsVersion,
                Map<ReviewCheckType, ReviewCheckExecutionMode> checks
        ) {
            this.requiredTermsVersion = requiredTermsVersion;
            this.checks = checks;
        }

        public String getRequiredTermsVersion() { return requiredTermsVersion; }
        public void setRequiredTermsVersion(String requiredTermsVersion) { this.requiredTermsVersion = requiredTermsVersion; }
        public Map<ReviewCheckType, ReviewCheckExecutionMode> getChecks() { return checks; }
        public void setChecks(Map<ReviewCheckType, ReviewCheckExecutionMode> checks) { this.checks = checks; }
        @Override public String requiredTermsVersion() { return requiredTermsVersion; }
        @Override public ReviewCheckExecutionMode modeFor(ReviewCheckType type) { return checks.get(type); }
    }
}
