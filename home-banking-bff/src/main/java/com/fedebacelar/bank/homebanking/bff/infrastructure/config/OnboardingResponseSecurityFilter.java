package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OnboardingResponseSecurityFilter extends OncePerRequestFilter {
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().contains("/onboarding")) {
            response.setHeader("Cache-Control", "no-store, max-age=0");
            String supplied = request.getHeader(CORRELATION_HEADER);
            response.setHeader(CORRELATION_HEADER, validCorrelationId(supplied) ? supplied : UUID.randomUUID().toString());
        }
        filterChain.doFilter(request, response);
    }

    private boolean validCorrelationId(String value) {
        return value != null && value.matches("^[A-Za-z0-9._-]{1,80}$");
    }
}
