package com.fedebacelar.bank.onboarding.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingTraceNoiseConfigurationTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String SPAN_ID = "0123456789abcdef";

    private final Sampler sampler =
            new OnboardingTraceNoiseConfiguration.RootDatabaseSpanNoiseFilter(Sampler.alwaysOn());

    @Test
    void dropsOnlyUnparentedDatabasePollingSpans() {
        assertThat(decision(Context.root(), "SELECT onboarding_db.onboarding_work_item", SpanKind.CLIENT))
                .isEqualTo(SamplingDecision.DROP);
        assertThat(decision(Context.root(), "onboarding.worker.provisioning", SpanKind.INTERNAL))
                .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    void preservesDatabaseSpansInsideARealTrace() {
        SpanContext parent = SpanContext.create(
                TRACE_ID,
                SPAN_ID,
                TraceFlags.getSampled(),
                TraceState.getDefault()
        );
        Context parentContext = Context.root().with(Span.wrap(parent));

        assertThat(decision(parentContext, "SELECT onboarding_db.onboarding_work_item", SpanKind.CLIENT))
                .isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    void respectsTheConfiguredSamplerForAllOtherSpans() {
        Sampler disabled = new OnboardingTraceNoiseConfiguration.RootDatabaseSpanNoiseFilter(Sampler.alwaysOff());

        assertThat(disabled.shouldSample(
                Context.root(),
                TRACE_ID,
                "GET /onboarding/applications",
                SpanKind.SERVER,
                Attributes.empty(),
                List.of()
        ).getDecision()).isEqualTo(SamplingDecision.DROP);
    }

    private SamplingDecision decision(Context parentContext, String name, SpanKind kind) {
        return sampler.shouldSample(
                parentContext,
                TRACE_ID,
                name,
                kind,
                Attributes.empty(),
                List.of()
        ).getDecision();
    }
}
