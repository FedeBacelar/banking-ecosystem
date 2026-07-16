package com.fedebacelar.bank.onboarding.infrastructure.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("observability")
public class W3cFeignTracePropagationInterceptor implements RequestInterceptor {

    private static final TextMapSetter<RequestTemplate> HEADER_SETTER =
            (request, key, value) -> request.header(key, value);

    private final TextMapPropagator propagator;

    public W3cFeignTracePropagationInterceptor(OpenTelemetry openTelemetry) {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public void apply(RequestTemplate template) {
        try {
            propagator.inject(Context.current(), template, HEADER_SETTER);
        } catch (RuntimeException ignored) {
            // Trace propagation must never prevent an internal request.
        }
    }
}
