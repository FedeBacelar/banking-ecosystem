package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.CredentialInvitationDeliveryEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CredentialInvitationDeliveryJpaRepository
        extends JpaRepository<CredentialInvitationDeliveryEntity, String> {
    Optional<CredentialInvitationDeliveryEntity> findByApplicationIdAndIdempotencyKeyHash(
            String applicationId,
            String idempotencyKeyHash
    );

    Optional<CredentialInvitationDeliveryEntity> findFirstByApplicationIdOrderByCreatedAtDesc(String applicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery from CredentialInvitationDeliveryEntity delivery
             where delivery.applicationId = :applicationId
               and delivery.status in :activeStatuses
             order by delivery.createdAt asc
            """)
    List<CredentialInvitationDeliveryEntity> findActiveByApplicationIdForUpdate(
            @Param("applicationId") String applicationId,
            @Param("activeStatuses") Set<WorkflowJobStatus> activeStatuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery from CredentialInvitationDeliveryEntity delivery
             where (delivery.status in :readyStatuses and delivery.nextAttemptAt <= :now)
                or (delivery.status = :runningStatus and delivery.lockedUntil <= :now)
             order by delivery.nextAttemptAt asc, delivery.createdAt asc
            """)
    List<CredentialInvitationDeliveryEntity> findClaimable(
            @Param("readyStatuses") Set<WorkflowJobStatus> readyStatuses,
            @Param("runningStatus") WorkflowJobStatus runningStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            select count(delivery) as pendingCount,
                   min(delivery.createdAt) as oldestCreatedAt
              from CredentialInvitationDeliveryEntity delivery
             where delivery.status in :activeStatuses
            """)
    CredentialInvitationBacklogProjection summarizeBacklog(
            @Param("activeStatuses") Set<WorkflowJobStatus> activeStatuses
    );
}
