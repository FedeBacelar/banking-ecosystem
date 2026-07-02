package com.fedebacelar.bank.account.infrastructure.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthorizationConfig {

    @Bean
    RequestInterceptor authorizationHeaderForwardingInterceptor() {
        return template -> currentRequest()
                .map(request -> request.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(value -> !value.isBlank())
                .ifPresent(value -> template.header(HttpHeaders.AUTHORIZATION, value));
    }

    private Optional<HttpServletRequest> currentRequest() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return Optional.of(servletRequestAttributes.getRequest());
        }
        return Optional.empty();
    }
}
