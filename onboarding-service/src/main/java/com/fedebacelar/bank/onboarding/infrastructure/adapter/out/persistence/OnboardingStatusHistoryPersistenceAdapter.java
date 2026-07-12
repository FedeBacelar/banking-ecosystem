package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingStatusHistoryPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingStatusHistoryJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class OnboardingStatusHistoryPersistenceAdapter implements OnboardingStatusHistoryRepositoryPort {
    private final OnboardingStatusHistoryJpaRepository repository;
    private final OnboardingStatusHistoryPersistenceMapper mapper;

    public OnboardingStatusHistoryPersistenceAdapter(OnboardingStatusHistoryJpaRepository repository, OnboardingStatusHistoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OnboardingStatusHistory save(OnboardingStatusHistory history) {
        return mapper.toDomain(repository.save(mapper.toEntity(history)));
    }
}
