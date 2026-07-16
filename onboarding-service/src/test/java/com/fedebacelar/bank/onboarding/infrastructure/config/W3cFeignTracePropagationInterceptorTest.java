package com.fedebacelar.bank.onboarding.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.RequestTemplate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.junit.jupiter.api.Test;

class W3cFeignTracePropagationInterceptorTest {

    @Test
    void propagationFailureNeverPreventsTheInternalRequest() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        ContextPropagators contextPropagators = mock(ContextPropagators.class);
        TextMapPropagator propagator = mock(TextMapPropagator.class);
        when(openTelemetry.getPropagators()).thenReturn(contextPropagators);
        when(contextPropagators.getTextMapPropagator()).thenReturn(propagator);
        doThrow(new IllegalStateException("export unavailable"))
                .when(propagator)
                .inject(any(), any(RequestTemplate.class), any());
        var interceptor = new W3cFeignTracePropagationInterceptor(openTelemetry);

        assertThatCode(() -> interceptor.apply(new RequestTemplate())).doesNotThrowAnyException();
    }
}
