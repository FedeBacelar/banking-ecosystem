package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingProvisioningStepPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingProvisioningStepJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingProvisioningStepPersistenceAdapter implements OnboardingProvisioningStepRepositoryPort {
    private final OnboardingProvisioningStepJpaRepository repository;
    private final OnboardingProvisioningStepPersistenceMapper mapper;
    public OnboardingProvisioningStepPersistenceAdapter(OnboardingProvisioningStepJpaRepository repository,
            OnboardingProvisioningStepPersistenceMapper mapper) { this.repository = repository; this.mapper = mapper; }
    @Override public OnboardingProvisioningStep save(OnboardingProvisioningStep step) {
        return mapper.toDomain(repository.save(mapper.toEntity(step)));
    }
    @Override public Optional<OnboardingProvisioningStep> findByApplicationIdAndStepType(UUID applicationId, ProvisioningStepType type) {
        return repository.findByApplicationIdAndStepType(applicationId.toString(), type).map(mapper::toDomain);
    }
    @Override public List<OnboardingProvisioningStep> findByApplicationId(UUID applicationId) {
        return repository.findByApplicationIdOrderByCreatedAtAsc(applicationId.toString()).stream().map(mapper::toDomain).toList();
    }
}
