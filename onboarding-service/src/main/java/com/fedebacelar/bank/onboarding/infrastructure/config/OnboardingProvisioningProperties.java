package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryPolicyPort;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onboarding.provisioning")
public class OnboardingProvisioningProperties implements OnboardingProvisioningPolicyPort,
        CredentialInvitationDeliveryPolicyPort {
    private int maxAttempts = 6;
    private Duration workerLease = Duration.ofMinutes(2);
    private int workerBatchSize = 20;
    private Duration credentialReconciliationDelay = Duration.ofSeconds(30);
    private Duration credentialSetupTimeout = Duration.ofDays(7);
    private Duration credentialInvitationCooldown = Duration.ofMinutes(1);
    private List<Duration> retryBackoff = List.of(Duration.ofSeconds(5), Duration.ofSeconds(30),
            Duration.ofMinutes(2), Duration.ofMinutes(10), Duration.ofMinutes(30));

    public Duration retryDelay(int attempt) {
        int index = Math.max(0, Math.min(attempt - 1, retryBackoff.size() - 1));
        return retryBackoff.get(index);
    }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public Duration getWorkerLease() { return workerLease; }
    public void setWorkerLease(Duration workerLease) { this.workerLease = workerLease; }
    public int getWorkerBatchSize() { return workerBatchSize; }
    public void setWorkerBatchSize(int workerBatchSize) { this.workerBatchSize = workerBatchSize; }
    public List<Duration> getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(List<Duration> retryBackoff) { this.retryBackoff = retryBackoff; }
    public Duration getCredentialReconciliationDelay() { return credentialReconciliationDelay; }
    public void setCredentialReconciliationDelay(Duration credentialReconciliationDelay) {
        this.credentialReconciliationDelay = credentialReconciliationDelay;
    }
    public Duration getCredentialSetupTimeout() { return credentialSetupTimeout; }
    public void setCredentialSetupTimeout(Duration credentialSetupTimeout) {
        this.credentialSetupTimeout = credentialSetupTimeout;
    }
    public Duration getCredentialInvitationCooldown() { return credentialInvitationCooldown; }
    public void setCredentialInvitationCooldown(Duration credentialInvitationCooldown) {
        this.credentialInvitationCooldown = credentialInvitationCooldown;
    }
    @Override public int maxAttempts() { return maxAttempts; }
    @Override public Duration workerLease() { return workerLease; }
    @Override public int workerBatchSize() { return workerBatchSize; }
    @Override public Duration credentialReconciliationDelay() { return credentialReconciliationDelay; }
    @Override public Duration credentialSetupTimeout() { return credentialSetupTimeout; }
    @Override public Duration credentialInvitationCooldown() { return credentialInvitationCooldown; }
}
