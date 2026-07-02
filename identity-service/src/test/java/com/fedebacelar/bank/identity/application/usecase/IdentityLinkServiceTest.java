package com.fedebacelar.bank.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.identity.application.command.CreateIdentityLinkCommand;
import com.fedebacelar.bank.identity.application.port.out.IdentityLinkRepositoryPort;
import com.fedebacelar.bank.identity.domain.enums.IdentityLinkStatus;
import com.fedebacelar.bank.identity.domain.enums.IdentityProvider;
import com.fedebacelar.bank.identity.domain.exception.DuplicateIdentityLinkException;
import com.fedebacelar.bank.identity.domain.exception.InactiveIdentityLinkException;
import com.fedebacelar.bank.identity.domain.model.IdentityLink;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityLinkServiceTest {

    private final IdentityLinkRepositoryPort repositoryPort = mock(IdentityLinkRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);
    private final IdentityLinkService service = new IdentityLinkService(repositoryPort, clock);

    @Test
    void createsIdentityLink() {
        UUID customerId = UUID.randomUUID();
        when(repositoryPort.existsByProviderAndProviderSubject(IdentityProvider.KEYCLOAK, "subject-1")).thenReturn(false);
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateIdentityLinkCommand(customerId, IdentityProvider.KEYCLOAK, "subject-1"));

        assertThat(created.customerId()).isEqualTo(customerId);
        assertThat(created.provider()).isEqualTo(IdentityProvider.KEYCLOAK);
        assertThat(created.status()).isEqualTo(IdentityLinkStatus.ACTIVE);
    }

    @Test
    void rejectsDuplicatedProviderSubject() {
        when(repositoryPort.existsByProviderAndProviderSubject(IdentityProvider.KEYCLOAK, "subject-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateIdentityLinkCommand(UUID.randomUUID(), IdentityProvider.KEYCLOAK, "subject-1")))
                .isInstanceOf(DuplicateIdentityLinkException.class);
    }

    @Test
    void resolvesOnlyActiveLinks() {
        IdentityLink disabled = new IdentityLink(
                UUID.randomUUID(),
                UUID.randomUUID(),
                IdentityProvider.KEYCLOAK,
                "subject-1",
                IdentityLinkStatus.DISABLED,
                Instant.now(clock),
                Instant.now(clock),
                0L
        );
        when(repositoryPort.findByProviderAndProviderSubject(IdentityProvider.KEYCLOAK, "subject-1")).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> service.resolveActive(IdentityProvider.KEYCLOAK, "subject-1"))
                .isInstanceOf(InactiveIdentityLinkException.class);
    }
}
