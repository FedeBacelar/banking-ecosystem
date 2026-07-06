package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingApplicationPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingApplicationJpaRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApplicationPersistenceAdapter implements OnboardingApplicationRepositoryPort {

    private final OnboardingApplicationJpaRepository repository;
    private final OnboardingApplicationPersistenceMapper mapper;

    public OnboardingApplicationPersistenceAdapter(
            OnboardingApplicationJpaRepository repository,
            OnboardingApplicationPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OnboardingApplication save(OnboardingApplication application) {
        return mapper.toDomain(repository.save(mapper.toEntity(application)));
    }

    @Override
    public Optional<OnboardingApplication> findById(UUID applicationId) {
        return repository.findById(applicationId.toString()).map(mapper::toDomain);
    }

    @Override
    public Optional<OnboardingApplication> findByMagicLinkTokenHash(String tokenHash) {
        return repository.findByMagicLinkTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<OnboardingApplication> findByContinuationTokenHash(String tokenHash) {
        return repository.findByContinuationTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<OnboardingApplication> findFirstByEmailAndStatusInOrderByCreatedAtDesc(String email, Set<OnboardingApplicationStatus> statuses) {
        return repository.findFirstByEmailAndStatusInOrderByCreatedAtDesc(email, statuses).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmailAndStatusIn(String email, Set<OnboardingApplicationStatus> statuses) {
        return repository.existsByEmailAndStatusIn(email, statuses);
    }
}
