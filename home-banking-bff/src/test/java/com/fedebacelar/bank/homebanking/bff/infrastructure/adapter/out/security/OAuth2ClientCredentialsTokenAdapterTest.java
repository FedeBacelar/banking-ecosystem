package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.exception.InternalAccessUnavailableException;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;

class OAuth2ClientCredentialsTokenAdapterTest {

    private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
    private final OAuth2ClientCredentialsTokenAdapter adapter =
            new OAuth2ClientCredentialsTokenAdapter(manager);

    @Test
    void shouldTranslateTokenEndpointFailuresToAStableApplicationException() {
        OAuth2AuthorizationException cause = new OAuth2AuthorizationException(
                new OAuth2Error("temporarily_unavailable")
        );
        when(manager.authorize(any())).thenThrow(cause);

        assertThatThrownBy(() -> adapter.getAccessToken(InternalAccessPurpose.ONBOARDING))
                .isInstanceOf(InternalAccessUnavailableException.class)
                .hasCause(cause);
    }

    @Test
    void shouldTreatAnEmptyAuthorizationResultAsUnavailable() {
        when(manager.authorize(any())).thenReturn(null);

        assertThatThrownBy(() -> adapter.getAccessToken(InternalAccessPurpose.ONBOARDING))
                .isInstanceOf(InternalAccessUnavailableException.class)
                .hasNoCause();
    }
}
