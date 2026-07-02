package com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.mapper;

import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence.entity.IdentityLinkEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class IdentityLinkPersistenceMapper {

    public IdentityLinkEntity toEntity(IdentityLink identityLink) {
        IdentityLinkEntity entity = new IdentityLinkEntity();
        entity.setId(identityLink.id().toString());
        entity.setCustomerId(identityLink.customerId().toString());
        entity.setProvider(identityLink.provider());
        entity.setProviderSubject(identityLink.providerSubject());
        entity.setStatus(identityLink.status());
        entity.setCreatedAt(identityLink.createdAt());
        entity.setUpdatedAt(identityLink.updatedAt());
        entity.setVersion(identityLink.version());
        return entity;
    }

    public IdentityLink toDomain(IdentityLinkEntity entity) {
        return new IdentityLink(
                UUID.fromString(entity.getId()),
                UUID.fromString(entity.getCustomerId()),
                entity.getProvider(),
                entity.getProviderSubject(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }
}
