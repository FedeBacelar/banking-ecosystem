package com.fedebacelar.bank.onboarding.infrastructure.config;

import com.fedebacelar.bank.onboarding.application.port.out.GetInternalAccessTokenPort;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class FeignAuthorizationConfig {
    @Bean
    RequestInterceptor onboardingServiceAuthorizationInterceptor(GetInternalAccessTokenPort accessTokenPort) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenPort.getAccessToken());
    }
}
