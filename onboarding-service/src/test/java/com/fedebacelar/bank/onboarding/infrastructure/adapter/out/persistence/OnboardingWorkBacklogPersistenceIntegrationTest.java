package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.TestcontainersConfiguration;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationDeliveryJpaRepository;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkItemJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "onboarding.review.worker-batch-size=0",
        "onboarding.provisioning.worker-batch-size=0"
})
@Import(TestcontainersConfiguration.class)
@Transactional
class OnboardingWorkBacklogPersistenceIntegrationTest {

    @Autowired
    private OnboardingApplicationRepositoryPort applications;

    @Autowired
    private OnboardingWorkItemRepositoryPort workItems;

    @Autowired
    private OnboardingWorkItemJpaRepository repository;

    @Autowired
    private CredentialInvitationDeliveryRepositoryPort credentialInvitations;

    @Autowired
    private CredentialInvitationDeliveryJpaRepository credentialInvitationRepository;

    @Test
    void aggregatesOnlyUnfinishedWorkByBoundedJobType() {
        Instant now = Instant.parse("2026-07-15T12:00:00Z");
        Set<WorkflowJobStatus> activeStatuses = Set.of(
                WorkflowJobStatus.PENDING,
                WorkflowJobStatus.RUNNING,
                WorkflowJobStatus.RETRY_WAIT
        );
        Map<WorkflowJobType, Long> before = counts(activeStatuses);
        String suffix = UUID.randomUUID().toString();
        OnboardingApplication application = applications.save(OnboardingApplication.start(
                suffix + "@example.com",
                "magic-" + suffix,
                now.plus(Duration.ofMinutes(30)),
                now.plus(Duration.ofDays(15)),
                now.minusSeconds(30)
        ));
        workItems.save(OnboardingWorkItem.pending(
                application.id(), WorkflowJobType.MAGIC_LINK_DELIVERY, now.minusSeconds(20)
        ));
        OnboardingWorkItem completed = OnboardingWorkItem.pending(
                application.id(), WorkflowJobType.AUTO_REVIEW, now.minusSeconds(10)
        ).claim(now.minusSeconds(5), Duration.ofMinutes(1)).succeed(now);
        workItems.save(completed);

        var rows = repository.summarizeBacklog(activeStatuses);
        Map<WorkflowJobType, Long> after = rows.stream().collect(Collectors.toMap(
                row -> row.getJobType(),
                row -> row.getPendingCount()
        ));

        assertThat(after.getOrDefault(WorkflowJobType.MAGIC_LINK_DELIVERY, 0L))
                .isEqualTo(before.getOrDefault(WorkflowJobType.MAGIC_LINK_DELIVERY, 0L) + 1L);
        assertThat(after.getOrDefault(WorkflowJobType.AUTO_REVIEW, 0L))
                .isEqualTo(before.getOrDefault(WorkflowJobType.AUTO_REVIEW, 0L));
        assertThat(rows.stream()
                .filter(row -> row.getJobType() == WorkflowJobType.MAGIC_LINK_DELIVERY)
                .findFirst().orElseThrow().getOldestCreatedAt())
                .isBeforeOrEqualTo(now.minusSeconds(20));
    }

    @Test
    void aggregatesOnlyActiveCredentialInvitationDeliveries() {
        Instant now = Instant.parse("2026-07-15T12:00:00Z");
        Set<WorkflowJobStatus> activeStatuses = activeStatuses();
        var before = credentialInvitationRepository.summarizeBacklog(activeStatuses);
        long beforeCount = before.getPendingCount();
        String suffix = UUID.randomUUID().toString();
        OnboardingApplication application = applications.save(OnboardingApplication.start(
                suffix + "@example.com",
                "magic-" + suffix,
                now.plus(Duration.ofMinutes(30)),
                now.plus(Duration.ofDays(15)),
                now.minusSeconds(60)
        ));
        credentialInvitations.save(CredentialInvitationDelivery.pending(
                application.id(), "pending-" + suffix, now.minusSeconds(40)
        ));
        CredentialInvitationDelivery completed = CredentialInvitationDelivery.pending(
                application.id(), "completed-" + suffix, now.minusSeconds(20)
        ).claim(now.minusSeconds(10), Duration.ofMinutes(1)).succeed(now);
        credentialInvitations.save(completed);

        var after = credentialInvitationRepository.summarizeBacklog(activeStatuses);

        assertThat(after.getPendingCount()).isEqualTo(beforeCount + 1L);
        assertThat(after.getOldestCreatedAt()).isBeforeOrEqualTo(now.minusSeconds(40));
    }

    private Map<WorkflowJobType, Long> counts(Set<WorkflowJobStatus> activeStatuses) {
        return repository.summarizeBacklog(activeStatuses).stream().collect(Collectors.toMap(
                row -> row.getJobType(),
                row -> row.getPendingCount()
        ));
    }

    private Set<WorkflowJobStatus> activeStatuses() {
        return Set.of(
                WorkflowJobStatus.PENDING,
                WorkflowJobStatus.RUNNING,
                WorkflowJobStatus.RETRY_WAIT
        );
    }
}
