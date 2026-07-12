package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingDocumentReferencePersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingDocumentReferenceJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingDocumentReferencePersistenceAdapter implements OnboardingDocumentReferenceRepositoryPort {

    private final OnboardingDocumentReferenceJpaRepository repository;
    private final OnboardingDocumentReferencePersistenceMapper mapper;

    public OnboardingDocumentReferencePersistenceAdapter(
            OnboardingDocumentReferenceJpaRepository repository,
            OnboardingDocumentReferencePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<OnboardingDocumentReference> findByApplicationIdAndCategory(UUID applicationId, OnboardingDocumentCategory category) {
        return repository.findByApplicationIdAndCategory(applicationId.toString(), category)
                .map(mapper::toDomain);
    }

    @Override
    public OnboardingDocumentReference save(OnboardingDocumentReference reference) {
        return mapper.toDomain(repository.save(mapper.toEntity(reference)));
    }
}
