package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingReviewMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.review")
public class OnboardingReviewProperties {
    private OnboardingReviewMode mode = OnboardingReviewMode.AUTO;
    private String policyVersion = "AR_DNI_SAVINGS_V1";
    private String requiredTermsVersion = "ONBOARDING_TERMS_AR_V1";
    private int maxAttempts = 6;
    private Duration workerLease = Duration.ofMinutes(2);
    private int workerBatchSize = 20;
    private List<Duration> retryBackoff = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofMinutes(2),
            Duration.ofMinutes(10), Duration.ofMinutes(30)
    );
    private Map<ReviewCheckType, ReviewCheckExecutionMode> checks = defaultChecks();

    @PostConstruct
    void validate() {
        if (mode != OnboardingReviewMode.AUTO) {
            throw new IllegalStateException("MANUAL onboarding review is not implemented.");
        }
        for (ReviewCheckType type : ReviewCheckType.values()) {
            if (!checks.containsKey(type)) {
                throw new IllegalStateException("Missing onboarding review mode for " + type);
            }
        }
        List<ReviewCheckType> localChecks = List.of(
                ReviewCheckType.DUPLICATE_CHECK,
                ReviewCheckType.DOCUMENTS_PRESENT,
                ReviewCheckType.TERMS_ACCEPTED,
                ReviewCheckType.BASIC_ELIGIBILITY
        );
        localChecks.forEach(type -> {
            if (checks.get(type) != ReviewCheckExecutionMode.LOCAL) {
                throw new IllegalStateException(type + " must use LOCAL review mode.");
            }
        });
        checks.forEach((type, executionMode) -> {
            if (executionMode == ReviewCheckExecutionMode.EXTERNAL) {
                throw new IllegalStateException("EXTERNAL review strategy is not implemented for " + type);
            }
        });
    }

    public Duration retryDelay(int attempt) {
        int index = Math.max(0, Math.min(attempt - 1, retryBackoff.size() - 1));
        return retryBackoff.get(index);
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
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public String getRequiredTermsVersion() { return requiredTermsVersion; }
    public void setRequiredTermsVersion(String requiredTermsVersion) { this.requiredTermsVersion = requiredTermsVersion; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getWorkerLease() { return workerLease; }
    public void setWorkerLease(Duration workerLease) { this.workerLease = workerLease; }
    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int workerBatchSize) { this.workerBatchSize = workerBatchSize; }
    public List<Duration> getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(List<Duration> retryBackoff) { this.retryBackoff = retryBackoff; }
    public Map<ReviewCheckType, ReviewCheckExecutionMode> getChecks() { return checks; }
    public void setChecks(Map<ReviewCheckType, ReviewCheckExecutionMode> checks) { this.checks = checks; }
}
