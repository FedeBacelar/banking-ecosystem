package com.fedebacelar.bank.account.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class FeignAuthorizationConfigTest {

    private final FeignAuthorizationConfig config = new FeignAuthorizationConfig();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void forwardsCurrentAuthorizationHeaderToFeignRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();

        config.authorizationHeaderForwardingInterceptor().apply(template);

        assertThat(template.headers())
                .containsEntry(HttpHeaders.AUTHORIZATION, List.of("Bearer test-token"));
    }

    @Test
    void leavesFeignRequestsUntouchedWhenNoAuthorizationHeaderExists() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        RequestTemplate template = new RequestTemplate();

        config.authorizationHeaderForwardingInterceptor().apply(template);

        assertThat(template.headers()).doesNotContainKey(HttpHeaders.AUTHORIZATION);
    }
}
