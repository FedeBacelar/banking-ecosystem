package com.fedebacelar.bank.gateway.observability;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("observability")
public class GatewayMetricsConfiguration {

    private static final String GATEWAY_ROUTE_ID_TAG = "spring.cloud.gateway.route.id";

    @Bean
    MeterFilter gatewayRouteClientObservationMeterFilter() {
        return MeterFilter.deny(id -> isClientRequestMetric(id.getName())
                && id.getTag(GATEWAY_ROUTE_ID_TAG) != null);
    }

    private boolean isClientRequestMetric(String metricName) {
        return "http.client.requests".equals(metricName)
                || "http.client.requests.active".equals(metricName);
    }
}
