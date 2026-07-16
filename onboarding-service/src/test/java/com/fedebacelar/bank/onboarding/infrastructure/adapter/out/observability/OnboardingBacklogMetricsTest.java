package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationBacklogProjection;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationDeliveryJpaRepository;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkBacklogProjection;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkItemJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingBacklogMetricsTest {
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    void refreshesFromOneAggregateQueryAndServesScrapesFromTheCache() {
        OnboardingWorkItemJpaRepository repository = mock(OnboardingWorkItemJpaRepository.class);
        CredentialInvitationDeliveryJpaRepository invitationRepository =
                mock(CredentialInvitationDeliveryJpaRepository.class);
        OnboardingWorkBacklogProjection row = mock(OnboardingWorkBacklogProjection.class);
        CredentialInvitationBacklogProjection invitationRow = mock(CredentialInvitationBacklogProjection.class);
        when(row.getJobType()).thenReturn(WorkflowJobType.AUTO_REVIEW);
        when(row.getPendingCount()).thenReturn(3L);
        when(row.getOldestCreatedAt()).thenReturn(NOW.minusSeconds(90));
        when(repository.summarizeBacklog(anySet())).thenReturn(List.of(row));
        when(invitationRow.getPendingCount()).thenReturn(4L);
        when(invitationRow.getOldestCreatedAt()).thenReturn(NOW.minusSeconds(120));
        when(invitationRepository.summarizeBacklog(anySet())).thenReturn(invitationRow);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var metrics = new OnboardingBacklogMetrics(
                repository,
                invitationRepository,
                registry,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        metrics.refresh();

        assertThat(registry.get("nerva.onboarding.work.pending")
                .tag("job_type", "auto_review").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("nerva.onboarding.work.oldest.age.seconds")
                .tag("job_type", "auto_review").gauge().value()).isEqualTo(90.0);
        assertThat(registry.get("nerva.onboarding.work.pending")
                .tag("job_type", "auto_review").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("nerva.onboarding.work.pending")
                .tag("job_type", "credential_invitation_delivery").gauge().value()).isEqualTo(4.0);
        assertThat(registry.get("nerva.onboarding.work.oldest.age.seconds")
                .tag("job_type", "credential_invitation_delivery").gauge().value()).isEqualTo(120.0);
        verify(repository, times(1)).summarizeBacklog(anySet());
        verify(invitationRepository, times(1)).summarizeBacklog(anySet());
    }

    @Test
    void keepsTheLastSnapshotWhenTheDatabaseRefreshFails() {
        OnboardingWorkItemJpaRepository repository = mock(OnboardingWorkItemJpaRepository.class);
        CredentialInvitationDeliveryJpaRepository invitationRepository =
                mock(CredentialInvitationDeliveryJpaRepository.class);
        OnboardingWorkBacklogProjection row = mock(OnboardingWorkBacklogProjection.class);
        CredentialInvitationBacklogProjection invitationRow = mock(CredentialInvitationBacklogProjection.class);
        when(row.getJobType()).thenReturn(WorkflowJobType.PROVISIONING);
        when(row.getPendingCount()).thenReturn(2L);
        when(row.getOldestCreatedAt()).thenReturn(NOW.minusSeconds(30));
        when(repository.summarizeBacklog(anySet())).thenReturn(List.of(row));
        when(invitationRow.getPendingCount()).thenReturn(5L);
        when(invitationRow.getOldestCreatedAt()).thenReturn(NOW.minusSeconds(45));
        when(invitationRepository.summarizeBacklog(anySet()))
                .thenReturn(invitationRow)
                .thenThrow(new IllegalStateException("database details must not escape"));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        var metrics = new OnboardingBacklogMetrics(
                repository,
                invitationRepository,
                registry,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        metrics.refresh();
        metrics.refresh();

        assertThat(registry.get("nerva.onboarding.work.pending")
                .tag("job_type", "provisioning").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("nerva.onboarding.work.oldest.age.seconds")
                .tag("job_type", "provisioning").gauge().value()).isEqualTo(30.0);
        assertThat(registry.get("nerva.onboarding.work.pending")
                .tag("job_type", "credential_invitation_delivery").gauge().value()).isEqualTo(5.0);
        assertThat(registry.get("nerva.onboarding.work.oldest.age.seconds")
                .tag("job_type", "credential_invitation_delivery").gauge().value()).isEqualTo(45.0);
        verify(repository, times(2)).summarizeBacklog(anySet());
        verify(invitationRepository, times(2)).summarizeBacklog(anySet());
    }
}
