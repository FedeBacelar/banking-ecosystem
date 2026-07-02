package com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence;

import com.fedebacelar.bank.identity.application.port.out.IdentityLinkRepositoryPort;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.mapper.IdentityLinkPersistenceMapper;
import com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.repository.IdentityLinkJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdentityLinkPersistenceAdapter implements IdentityLinkRepositoryPort {

    private final IdentityLinkJpaRepository jpaRepository;
    private final IdentityLinkPersistenceMapper mapper;

    public IdentityLinkPersistenceAdapter(IdentityLinkJpaRepository jpaRepository, IdentityLinkPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public IdentityLink save(IdentityLink identityLink) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(identityLink)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityLink> findById(UUID identityLinkId) {
        return jpaRepository.findById(identityLinkId.toString()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityLink> findByProviderAndProviderSubject(IdentityProvider provider, String providerSubject) {
        return jpaRepository.findByProviderAndProviderSubject(provider, providerSubject).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityLink> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId.toString()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProviderAndProviderSubject(IdentityProvider provider, String providerSubject) {
        return jpaRepository.existsByProviderAndProviderSubject(provider, providerSubject);
    }
}
