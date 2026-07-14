package com.fedebacelar.bank.account.infrastructure.config;

import com.fedebacelar.bank.account.application.port.out.GetInternalAccessTokenPort;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class FeignAuthorizationConfig {

    @Bean
    RequestInterceptor accountServiceAuthorizationInterceptor(GetInternalAccessTokenPort accessTokenPort) {
        return template -> {
            template.removeHeader(HttpHeaders.AUTHORIZATION);
            String accessToken = accessTokenPort.getAccessToken();
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        };
    }
}
