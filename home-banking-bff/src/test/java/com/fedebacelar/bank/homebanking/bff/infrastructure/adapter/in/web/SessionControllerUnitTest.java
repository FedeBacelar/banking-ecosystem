package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.AuthenticatedUserResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;

class SessionControllerUnitTest {

    private final GetAuthenticatedHomeContextUseCase useCase = mock(GetAuthenticatedHomeContextUseCase.class);
    private final SessionController controller = new SessionController(useCase);

    @Test
    void shouldMaterializeCsrfTokenAfterResolvingTheBankingContext() {
        OidcUser oidcUser = mock(OidcUser.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        AuthenticatedUser user = new AuthenticatedUser("subject", "username", "user@example.com");
        when(oidcUser.getSubject()).thenReturn(user.subject());
        when(oidcUser.getPreferredUsername()).thenReturn(user.username());
        when(oidcUser.getEmail()).thenReturn(user.email());
        when(oidcUser.getFullName()).thenReturn("Customer Name");
        when(useCase.getHomeContext(user)).thenReturn(new HomeBankingContext(
                user.subject(), user.username(), user.email(), UUID.randomUUID(), null, List.of()
        ));

        AuthenticatedUserResponse response = controller.me(oidcUser, csrfToken);

        assertThat(response).isEqualTo(new AuthenticatedUserResponse("username", "Customer Name"));
        verify(useCase).getHomeContext(user);
        verify(csrfToken).getToken();
    }
}
