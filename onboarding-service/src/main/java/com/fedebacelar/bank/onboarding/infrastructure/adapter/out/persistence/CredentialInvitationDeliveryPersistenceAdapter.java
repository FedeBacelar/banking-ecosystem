package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.CredentialInvitationDeliveryPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.CredentialInvitationDeliveryJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CredentialInvitationDeliveryPersistenceAdapter
        implements CredentialInvitationDeliveryRepositoryPort {
    private static final Set<WorkflowJobStatus> READY = Set.of(
            WorkflowJobStatus.PENDING, WorkflowJobStatus.RETRY_WAIT
    );

    private final CredentialInvitationDeliveryJpaRepository repository;
    private final CredentialInvitationDeliveryPersistenceMapper mapper;

    public CredentialInvitationDeliveryPersistenceAdapter(
            CredentialInvitationDeliveryJpaRepository repository,
            CredentialInvitationDeliveryPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public CredentialInvitationDelivery save(CredentialInvitationDelivery delivery) {
        return mapper.toDomain(repository.save(mapper.toEntity(delivery)));
    }

    @Override
    public Optional<CredentialInvitationDelivery> findByApplicationIdAndIdempotencyKeyHash(
            UUID applicationId,
            String idempotencyKeyHash
    ) {
        return repository.findByApplicationIdAndIdempotencyKeyHash(
                applicationId.toString(), idempotencyKeyHash
        ).map(mapper::toDomain);
    }

    @Override
    public Optional<CredentialInvitationDelivery> findLatestByApplicationId(UUID applicationId) {
        return repository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId.toString())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public List<CredentialInvitationDelivery> findActiveByApplicationIdForUpdate(UUID applicationId) {
        return repository.findActiveByApplicationIdForUpdate(
                        applicationId.toString(),
                        Set.of(
                                WorkflowJobStatus.PENDING,
                                WorkflowJobStatus.RUNNING,
                                WorkflowJobStatus.RETRY_WAIT
                        )
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<CredentialInvitationDelivery> claimNext(Instant now, Duration lease) {
        return repository.findClaimable(
                        READY, WorkflowJobStatus.RUNNING, now, PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(mapper::toDomain)
                .map(delivery -> mapper.toDomain(repository.saveAndFlush(
                        mapper.toEntity(delivery.claim(now, lease))
                )));
    }
}
