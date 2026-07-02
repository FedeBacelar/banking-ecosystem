package com.fedebacelar.bank.identity.domain.model;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.exception.InvalidIdentityLinkStatusTransitionException;
import java.time.Instant;
import java.util.UUID;

public record IdentityLink(
        UUID id,
        UUID customerId,
        IdentityProvider provider,
        String providerSubject,
        IdentityLinkStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {

    public static IdentityLink create(
            UUID customerId,
            IdentityProvider provider,
            String providerSubject,
            Instant createdAt
    ) {
        return new IdentityLink(
                UUID.randomUUID(),
                customerId,
                provider,
                providerSubject,
                IdentityLinkStatus.ACTIVE,
                createdAt,
                createdAt,
                null
        );
    }

    public IdentityLink activate(Instant changedAt) {
        if (status == IdentityLinkStatus.ACTIVE) {
            return this;
        }
        if (status != IdentityLinkStatus.PENDING_VERIFICATION && status != IdentityLinkStatus.DISABLED) {
            throw new InvalidIdentityLinkStatusTransitionException(id, status, IdentityLinkStatus.ACTIVE);
        }
        return withStatus(IdentityLinkStatus.ACTIVE, changedAt);
    }

    public IdentityLink disable(Instant changedAt) {
        if (status == IdentityLinkStatus.DISABLED) {
            return this;
        }
        if (status != IdentityLinkStatus.ACTIVE && status != IdentityLinkStatus.PENDING_VERIFICATION) {
            throw new InvalidIdentityLinkStatusTransitionException(id, status, IdentityLinkStatus.DISABLED);
        }
        return withStatus(IdentityLinkStatus.DISABLED, changedAt);
    }

    private IdentityLink withStatus(IdentityLinkStatus newStatus, Instant changedAt) {
        return new IdentityLink(id, customerId, provider, providerSubject, newStatus, createdAt, changedAt, version);
    }
}
