package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryPolicyPort;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.notification")
public class OnboardingNotificationProperties implements MagicLinkDeliveryPolicyPort {
    private int maxAttempts = 6;
    private int workerBatchSize = 20;
    private Duration workerLease = Duration.ofMinutes(1);
    private List<Duration> retryBackoff = List.of(
            Duration.ofSeconds(5), Duration.ofSeconds(30), Duration.ofMinutes(2),
            Duration.ofMinutes(10), Duration.ofMinutes(30)
    );

    public Duration retryDelay(int attempt) {
        int index = Math.max(0, Math.min(attempt - 1, retryBackoff.size() - 1));
        return retryBackoff.get(index);
    }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int workerBatchSize) { this.workerBatchSize = workerBatchSize; }
    public Duration getWorkerLease() { return workerLease; }
    public void setWorkerLease(Duration workerLease) { this.workerLease = workerLease; }
    public List<Duration> getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(List<Duration> retryBackoff) { this.retryBackoff = retryBackoff; }
    @Override public int maxAttempts() { return maxAttempts; }
    @Override public int workerBatchSize() { return workerBatchSize; }
    @Override public Duration workerLease() { return workerLease; }
}
