package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewCheckRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingReviewCheck;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingReviewCheckPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingReviewCheckJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingReviewCheckPersistenceAdapter implements OnboardingReviewCheckRepositoryPort {
    private final OnboardingReviewCheckJpaRepository repository;
    private final OnboardingReviewCheckPersistenceMapper mapper;

    public OnboardingReviewCheckPersistenceAdapter(OnboardingReviewCheckJpaRepository repository, OnboardingReviewCheckPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public OnboardingReviewCheck save(OnboardingReviewCheck check) {
        return mapper.toDomain(repository.save(mapper.toEntity(check)));
    }

    @Override
    public List<OnboardingReviewCheck> findByApplicationId(UUID applicationId) {
        return repository.findByApplicationIdOrderByCreatedAtAsc(applicationId.toString()).stream().map(mapper::toDomain).toList();
    }
}
