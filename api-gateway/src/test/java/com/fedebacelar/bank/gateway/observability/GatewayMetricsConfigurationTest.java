package com.fedebacelar.bank.gateway.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.junit.jupiter.api.Test;

class GatewayMetricsConfigurationTest {

    private static final String ROUTE_ID_TAG = "spring.cloud.gateway.route.id";

    private final MeterFilter meterFilter = new GatewayMetricsConfiguration()
            .gatewayRouteClientObservationMeterFilter();

    @Test
    void shouldDenyOnlyGatewayRouteClientRequestMetrics() {
        assertThat(meterFilter.accept(meterId("http.client.requests", ROUTE_ID_TAG, "home-banking-bff")))
                .isEqualTo(MeterFilterReply.DENY);
        assertThat(meterFilter.accept(meterId("http.client.requests.active", ROUTE_ID_TAG, "home-banking-bff")))
                .isEqualTo(MeterFilterReply.DENY);
    }

    @Test
    void shouldKeepGenericClientAndCanonicalGatewayMetrics() {
        assertThat(meterFilter.accept(meterId("http.client.requests", "client.name", "localhost")))
                .isEqualTo(MeterFilterReply.NEUTRAL);
        assertThat(meterFilter.accept(meterId("spring.cloud.gateway.requests", ROUTE_ID_TAG, "home-banking-bff")))
                .isEqualTo(MeterFilterReply.NEUTRAL);
    }

    private Meter.Id meterId(String name, String tagKey, String tagValue) {
        return new Meter.Id(name, Tags.of(tagKey, tagValue), null, null, Meter.Type.OTHER);
    }
}
