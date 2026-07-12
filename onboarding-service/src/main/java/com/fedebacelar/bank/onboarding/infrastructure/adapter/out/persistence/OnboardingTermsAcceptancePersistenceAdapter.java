package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingTermsAcceptancePersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingTermsAcceptanceJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingTermsAcceptancePersistenceAdapter implements OnboardingTermsAcceptanceRepositoryPort {

    private final OnboardingTermsAcceptanceJpaRepository repository;
    private final OnboardingTermsAcceptancePersistenceMapper mapper;

    public OnboardingTermsAcceptancePersistenceAdapter(
            OnboardingTermsAcceptanceJpaRepository repository,
            OnboardingTermsAcceptancePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OnboardingTermsAcceptance> findByApplicationId(UUID applicationId) {
        return repository.findById(applicationId.toString())
                .map(mapper::toDomain);
    }

    @Override
    public OnboardingTermsAcceptance save(OnboardingTermsAcceptance acceptance) {
        return mapper.toDomain(repository.save(mapper.toEntity(acceptance)));
    }
}
