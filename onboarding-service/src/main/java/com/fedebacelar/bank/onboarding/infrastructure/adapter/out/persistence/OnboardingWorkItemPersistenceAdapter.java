package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingWorkItemPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingWorkItemJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Component
public class OnboardingWorkItemPersistenceAdapter implements OnboardingWorkItemRepositoryPort {
    private static final Set<WorkflowJobStatus> READY = Set.of(WorkflowJobStatus.PENDING, WorkflowJobStatus.RETRY_WAIT);

    private final OnboardingWorkItemJpaRepository repository;
    private final OnboardingWorkItemPersistenceMapper mapper;

    public OnboardingWorkItemPersistenceAdapter(OnboardingWorkItemJpaRepository repository, OnboardingWorkItemPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OnboardingWorkItem save(OnboardingWorkItem workItem) {
        return mapper.toDomain(repository.save(mapper.toEntity(workItem)));
    }

    @Override
    public Optional<OnboardingWorkItem> findByApplicationIdAndJobType(UUID applicationId, WorkflowJobType jobType) {
        return repository.findByApplicationIdAndJobType(applicationId.toString(), jobType).map(mapper::toDomain);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OnboardingWorkItem> claimNext(WorkflowJobType jobType, Instant now, Duration lease) {
        return repository.findClaimable(jobType, READY, WorkflowJobStatus.RUNNING, now, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(mapper::toDomain)
                .map(item -> mapper.toDomain(repository.saveAndFlush(mapper.toEntity(item.claim(now, lease)))));
    }
}
