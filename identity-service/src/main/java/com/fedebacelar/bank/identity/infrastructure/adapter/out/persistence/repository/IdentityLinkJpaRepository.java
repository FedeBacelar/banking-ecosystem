package com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.repository;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.entity.IdentityLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityLinkJpaRepository extends JpaRepository<IdentityLinkEntity, String> {

    Optional<IdentityLinkEntity> findByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);

    List<IdentityLinkEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    boolean existsByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);
}
