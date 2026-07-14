package com.fedebacelar.bank.account.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.account.application.port.out.GetInternalAccessTokenPort;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class FeignAuthorizationConfigTest {

    private final FeignAuthorizationConfig config = new FeignAuthorizationConfig();
    private final GetInternalAccessTokenPort accessTokenPort = mock(GetInternalAccessTokenPort.class);

    @Test
    void authenticatesFeignRequestsWithTheAccountServiceToken() {
        when(accessTokenPort.getAccessToken()).thenReturn("account-service-token");
        RequestInterceptor interceptor = config.accountServiceAuthorizationInterceptor(accessTokenPort);
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers())
                .containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer account-service-token"));
    }

    @Test
    void replacesAnyExistingAuthorizationHeaderInsteadOfForwardingIt() {
        when(accessTokenPort.getAccessToken()).thenReturn("account-service-token");
        RequestInterceptor interceptor = config.accountServiceAuthorizationInterceptor(accessTokenPort);
        RequestTemplate template = new RequestTemplate();
        template.header(HttpHeaders.AUTHORIZATION, "Bearer incoming-user-token");

        interceptor.apply(template);

        assertThat(template.headers())
                .containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer account-service-token"));
    }

    @Test
    void failsClosedWhenTheTechnicalTokenCannotBeObtained() {
        when(accessTokenPort.getAccessToken()).thenThrow(new IllegalStateException("authorization unavailable"));
        RequestInterceptor interceptor = config.accountServiceAuthorizationInterceptor(accessTokenPort);
        RequestTemplate template = new RequestTemplate();
        template.header(HttpHeaders.AUTHORIZATION, "Bearer incoming-user-token");

        assertThatThrownBy(() -> interceptor.apply(template))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("authorization unavailable");
        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }
}
