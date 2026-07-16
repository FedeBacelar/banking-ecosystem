package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort.WorkType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationBacklogProjection;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationDeliveryJpaRepository;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkBacklogProjection;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkItemJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("observability")
public class OnboardingBacklogMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnboardingBacklogMetrics.class);
    private static final Set<WorkflowJobStatus> ACTIVE_STATUSES = Set.of(
            WorkflowJobStatus.PENDING,
            WorkflowJobStatus.RUNNING,
            WorkflowJobStatus.RETRY_WAIT
    );

    private final OnboardingWorkItemJpaRepository workItemRepository;
    private final CredentialInvitationDeliveryJpaRepository credentialInvitationRepository;
    private final Clock clock;
    private final AtomicReference<Map<WorkType, BacklogValue>> cache;

    public OnboardingBacklogMetrics(
            OnboardingWorkItemJpaRepository repository,
            CredentialInvitationDeliveryJpaRepository credentialInvitationRepository,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.workItemRepository = repository;
        this.credentialInvitationRepository = credentialInvitationRepository;
        this.clock = clock;
        this.cache = new AtomicReference<>(emptySnapshot());
        registerGauges(meterRegistry);
    }

    @Scheduled(
            fixedDelayString = "${observability.onboarding.backlog-refresh-interval:PT15S}",
            initialDelayString = "${observability.onboarding.backlog-initial-delay:PT15S}"
    )
    public void refresh() {
        try {
            Instant now = Instant.now(clock);
            Map<WorkType, BacklogValue> refreshed = new EnumMap<>(emptySnapshot());
            for (OnboardingWorkBacklogProjection row : workItemRepository.summarizeBacklog(ACTIVE_STATUSES)) {
                refreshed.put(
                        WorkType.from(row.getJobType()),
                        backlogValue(row.getPendingCount(), row.getOldestCreatedAt(), now)
                );
            }
            CredentialInvitationBacklogProjection invitationBacklog =
                    credentialInvitationRepository.summarizeBacklog(ACTIVE_STATUSES);
            refreshed.put(
                    WorkType.CREDENTIAL_INVITATION_DELIVERY,
                    backlogValue(
                            invitationBacklog.getPendingCount(),
                            invitationBacklog.getOldestCreatedAt(),
                            now
                    )
            );
            cache.set(Map.copyOf(refreshed));
        } catch (RuntimeException failure) {
            LOGGER.atWarn()
                    .addKeyValue("event.name", "onboarding.backlog.refresh_failed")
                    .addKeyValue("error.type", failure.getClass().getSimpleName())
                    .log("Onboarding backlog refresh failed");
        }
    }

    private BacklogValue backlogValue(long pendingCount, Instant oldestCreatedAt, Instant now) {
        long ageSeconds = oldestCreatedAt == null
                ? 0L
                : Math.max(0L, Duration.between(oldestCreatedAt, now).toSeconds());
        return new BacklogValue(pendingCount, ageSeconds);
    }

    private void registerGauges(MeterRegistry meterRegistry) {
        for (WorkType workType : WorkType.values()) {
            String metricValue = workType.name().toLowerCase(Locale.ROOT);
            Gauge.builder("nerva.onboarding.work.pending", cache,
                            current -> current.get().get(workType).pendingCount())
                    .description("Current unfinished onboarding work items")
                    .tag("job_type", metricValue)
                    .register(meterRegistry);
            Gauge.builder("nerva.onboarding.work.oldest.age.seconds", cache,
                            current -> current.get().get(workType).oldestAgeSeconds())
                    .description("Age in seconds of the oldest unfinished onboarding work item")
                    .tag("job_type", metricValue)
                    .register(meterRegistry);
        }
    }

    private Map<WorkType, BacklogValue> emptySnapshot() {
        Map<WorkType, BacklogValue> empty = new EnumMap<>(WorkType.class);
        for (WorkType workType : WorkType.values()) {
            empty.put(workType, new BacklogValue(0L, 0L));
        }
        return empty;
    }

    private record BacklogValue(long pendingCount, long oldestAgeSeconds) {
    }
}
