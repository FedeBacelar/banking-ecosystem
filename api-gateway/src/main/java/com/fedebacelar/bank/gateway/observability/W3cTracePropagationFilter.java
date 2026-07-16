package com.fedebacelar.bank.gateway.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Propagates the Gateway server span to the proxied service. */
@Component
@Profile("observability")
public class W3cTracePropagationFilter implements GlobalFilter, Ordered {
    private final TextMapPropagator propagator;

    public W3cTracePropagationFilter(OpenTelemetry openTelemetry) {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return Mono.defer(() -> {
            try {
                Context context = Context.current();
                if (!Span.fromContext(context).getSpanContext().isValid()) {
                    return chain.filter(exchange);
                }

                ServerWebExchange tracedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .headers(headers -> propagator.inject(
                                        context,
                                        headers,
                                        HttpHeaders::set
                                ))
                                .build())
                        .build();
                return chain.filter(tracedExchange);
            } catch (RuntimeException telemetryFailure) {
                return chain.filter(exchange);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
