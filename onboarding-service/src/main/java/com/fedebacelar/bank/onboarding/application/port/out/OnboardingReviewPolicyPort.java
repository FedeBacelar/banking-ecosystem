package com.fedebacelar.bank.onboarding.application.port.out;

import com.fedebacelar.bank.onboarding.domain.enums.OnboardingReviewMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckExecutionMode;
import com.fedebacelar.bank.onboarding.domain.enums.ReviewCheckType;
import java.time.Duration;
import java.time.ZoneId;

public interface OnboardingReviewPolicyPort {

    OnboardingReviewMode mode();

    String activePolicyVersion();

    ReviewPolicy policy(String version);

    int maxAttempts();

    Duration workerLease();

    int workerBatchSize();

    Duration retryDelay(int attempt);

    ZoneId businessZone();

    interface ReviewPolicy {
        String requiredTermsVersion();

        ReviewCheckExecutionMode modeFor(ReviewCheckType type);
    }
}
