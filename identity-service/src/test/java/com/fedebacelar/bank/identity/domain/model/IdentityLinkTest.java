package com.fedebacelar.bank.identity.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityLinkTest {

    @Test
    void createsActiveIdentityLink() {
        Instant now = Instant.parse("2026-07-02T00:00:00Z");

        IdentityLink identityLink = IdentityLink.create(UUID.randomUUID(), IdentityProvider.KEYCLOAK, "keycloak-sub", now);

        assertThat(identityLink.id()).isNotNull();
        assertThat(identityLink.status()).isEqualTo(IdentityLinkStatus.ACTIVE);
        assertThat(identityLink.createdAt()).isEqualTo(now);
        assertThat(identityLink.updatedAt()).isEqualTo(now);
    }

    @Test
    void disablesActiveIdentityLink() {
        Instant now = Instant.parse("2026-07-02T00:00:00Z");
        Instant changedAt = Instant.parse("2026-07-03T00:00:00Z");
        IdentityLink identityLink = IdentityLink.create(UUID.randomUUID(), IdentityProvider.KEYCLOAK, "keycloak-sub", now);

        IdentityLink disabled = identityLink.disable(changedAt);

        assertThat(disabled.status()).isEqualTo(IdentityLinkStatus.DISABLED);
        assertThat(disabled.updatedAt()).isEqualTo(changedAt);
    }
}
