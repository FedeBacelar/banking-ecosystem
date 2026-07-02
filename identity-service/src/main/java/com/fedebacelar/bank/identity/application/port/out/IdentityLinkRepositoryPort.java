package com.fedebacelar.bank.identity.application.port.out;

import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityLinkRepositoryPort {

    IdentityLink save(IdentityLink identityLink);

    Optional<IdentityLink> findById(UUID identityLinkId);

    Optional<IdentityLink> findByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);

    List<IdentityLink> findByCustomerId(UUID customerId);

    boolean existsByProviderAndProviderSubject(IdentityProvider provider, String providerSubject);
}
