package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.entity.OnboardingWorkItemEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OnboardingWorkItemJpaRepository extends JpaRepository<OnboardingWorkItemEntity, String> {
    Optional<OnboardingWorkItemEntity> findByApplicationIdAndJobType(String applicationId, WorkflowJobType jobType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from OnboardingWorkItemEntity item
             where item.jobType = :jobType
               and ((item.status in :readyStatuses and item.nextAttemptAt <= :now)
                    or (item.status = :runningStatus and item.lockedUntil <= :now))
             order by item.nextAttemptAt asc, item.createdAt asc
            """)
    List<OnboardingWorkItemEntity> findClaimable(
            @Param("jobType") WorkflowJobType jobType,
            @Param("readyStatuses") Set<WorkflowJobStatus> readyStatuses,
            @Param("runningStatus") WorkflowJobStatus runningStatus,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            select item.jobType as jobType,
                   count(item) as pendingCount,
                   min(item.createdAt) as oldestCreatedAt
              from OnboardingWorkItemEntity item
             where item.status in :activeStatuses
             group by item.jobType
            """)
    List<OnboardingWorkBacklogProjection> summarizeBacklog(
            @Param("activeStatuses") Set<WorkflowJobStatus> activeStatuses
    );
}
