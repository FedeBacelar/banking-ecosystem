package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.OnboardingApplicantDataPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.OnboardingApplicantDataJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OnboardingApplicantDataPersistenceAdapter implements OnboardingApplicantDataRepositoryPort {

    private final OnboardingApplicantDataJpaRepository repository;
    private final OnboardingApplicantDataPersistenceMapper mapper;

    public OnboardingApplicantDataPersistenceAdapter(
            OnboardingApplicantDataJpaRepository repository,
            OnboardingApplicantDataPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ApplicantData save(ApplicantData applicantData) {
        return mapper.toDomain(repository.save(mapper.toEntity(applicantData)));
    }

    @Override
    public Optional<ApplicantData> findByApplicationId(UUID applicationId) {
        return repository.findById(applicationId.toString()).map(mapper::toDomain);
    }
}
