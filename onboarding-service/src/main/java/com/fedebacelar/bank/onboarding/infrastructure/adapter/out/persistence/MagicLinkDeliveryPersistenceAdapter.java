package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.mapper.MagicLinkDeliveryPersistenceMapper;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence.repository.MagicLinkDeliveryJpaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MagicLinkDeliveryPersistenceAdapter implements MagicLinkDeliveryRepositoryPort {
    private final MagicLinkDeliveryJpaRepository repository;
    private final MagicLinkDeliveryPersistenceMapper mapper;

    public MagicLinkDeliveryPersistenceAdapter(
            MagicLinkDeliveryJpaRepository repository,
            MagicLinkDeliveryPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<MagicLinkDelivery> findByApplicationId(UUID applicationId) {
        return repository.findById(applicationId.toString()).map(mapper::toDomain);
    }

    @Override
    public MagicLinkDelivery save(MagicLinkDelivery delivery) {
        return mapper.toDomain(repository.save(mapper.toEntity(delivery)));
    }
}
