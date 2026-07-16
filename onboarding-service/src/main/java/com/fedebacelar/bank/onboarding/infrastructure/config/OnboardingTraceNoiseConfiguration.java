package com.fedebacelar.bank.onboarding.infrastructure.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("observability")
public class OnboardingTraceNoiseConfiguration {

    @Bean
    AutoConfigurationCustomizerProvider onboardingSamplerCustomizer() {
        return customizer -> customizer.addSamplerCustomizer(
                (configuredSampler, properties) -> new RootDatabaseSpanNoiseFilter(configuredSampler)
        );
    }

    static final class RootDatabaseSpanNoiseFilter implements Sampler {
        private static final List<String> DATABASE_OPERATION_PREFIXES = List.of(
                "select ", "insert ", "update ", "delete ", "merge ", "call "
        );

        private final Sampler delegate;

        RootDatabaseSpanNoiseFilter(Sampler delegate) {
            this.delegate = delegate;
        }

        @Override
        public SamplingResult shouldSample(
                Context parentContext,
                String traceId,
                String name,
                SpanKind spanKind,
                Attributes attributes,
                List<LinkData> parentLinks
        ) {
            if (isUnparentedDatabaseOperation(parentContext, name, spanKind)) {
                return SamplingResult.drop();
            }
            return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
        }

        @Override
        public String getDescription() {
            return "DropRootDatabaseSpanNoise{" + delegate.getDescription() + '}';
        }

        private boolean isUnparentedDatabaseOperation(Context parentContext, String name, SpanKind spanKind) {
            if (spanKind != SpanKind.CLIENT || Span.fromContext(parentContext).getSpanContext().isValid()) {
                return false;
            }
            String normalizedName = name.toLowerCase(Locale.ROOT);
            return DATABASE_OPERATION_PREFIXES.stream().anyMatch(normalizedName::startsWith);
        }
    }
}
