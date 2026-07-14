package com.fedebacelar.bank.gateway.ratelimit;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
final class OnboardingStartRateLimitFilter implements GlobalFilter, Ordered {

    private static final String TARGET_PATH = "/web/onboarding/applications";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final Pattern SAFE_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final byte[] REJECTION_BODY = """
            {"type":"about:blank","title":"Demasiadas solicitudes","status":429,"detail":"Esperá antes de pedir otro enlace.","code":"ONBOARDING_START_RATE_LIMIT"}
            """.strip().getBytes(StandardCharsets.UTF_8);

    private final OnboardingStartRateLimitProperties properties;
    private final TrustedClientIpResolver clientIpResolver;
    private final LocalOnboardingStartRateLimiter rateLimiter;

    OnboardingStartRateLimitFilter(
            OnboardingStartRateLimitProperties properties,
            TrustedClientIpResolver clientIpResolver,
            LocalOnboardingStartRateLimiter rateLimiter
    ) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.enabled() || !isOnboardingStart(exchange)) {
            return chain.filter(exchange);
        }

        LocalOnboardingStartRateLimiter.Decision decision = rateLimiter.acquire(
                clientIpResolver.resolve(exchange.getRequest())
        );
        if (decision.allowed()) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.setCacheControl("no-store");
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        headers.set(CORRELATION_HEADER, correlationId(exchange));
        headers.setContentLength(REJECTION_BODY.length);
        DataBuffer body = exchange.getResponse().bufferFactory().wrap(REJECTION_BODY);
        return exchange.getResponse().writeWith(Mono.just(body));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isOnboardingStart(ServerWebExchange exchange) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        try {
            String decodedPath = UriUtils.decode(
                    exchange.getRequest().getURI().getRawPath(),
                    StandardCharsets.UTF_8
            );
            return TARGET_PATH.equals(withoutMatrixParameters(decodedPath));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String withoutMatrixParameters(String path) {
        StringBuilder result = new StringBuilder(path.length());
        boolean insideParameters = false;
        for (int index = 0; index < path.length(); index++) {
            char current = path.charAt(index);
            if (current == ';') {
                insideParameters = true;
            } else if (current == '/') {
                insideParameters = false;
                result.append(current);
            } else if (!insideParameters) {
                result.append(current);
            }
        }
        return result.toString();
    }

    private String correlationId(ServerWebExchange exchange) {
        String supplied = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
        return supplied != null && SAFE_CORRELATION_ID.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
    }
}
