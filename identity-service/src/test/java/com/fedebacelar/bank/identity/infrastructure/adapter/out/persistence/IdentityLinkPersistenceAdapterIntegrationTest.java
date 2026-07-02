package com.fedebacelar.bank.identity.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.identity.TestcontainersConfiguration;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class IdentityLinkPersistenceAdapterIntegrationTest {

    @Autowired
    private IdentityLinkPersistenceAdapter adapter;

    @Test
    void savesAndFindsIdentityLink() {
        UUID customerId = UUID.randomUUID();
        IdentityLink identityLink = IdentityLink.create(customerId, IdentityProvider.KEYCLOAK, "keycloak-sub-1", Instant.parse("2026-07-02T00:00:00Z"));

        adapter.save(identityLink);

        assertThat(adapter.findByProviderAndProviderSubject(IdentityProvider.KEYCLOAK, "keycloak-sub-1")).isPresent();
        assertThat(adapter.findByCustomerId(customerId)).hasSize(1);
        assertThat(adapter.existsByProviderAndProviderSubject(IdentityProvider.KEYCLOAK, "keycloak-sub-1")).isTrue();
    }
}
