package com.fedebacelar.bank.homebanking.bff.infrastructure.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient internalServiceWebClient(
            WebClient.Builder autoConfiguredWebClientBuilder,
            ReactorLoadBalancerExchangeFilterFunction loadBalancerFilter,
            @Value("${home-banking-bff.http.connect-timeout:PT3S}") Duration connectTimeout,
            @Value("${home-banking-bff.http.response-timeout:PT30S}") Duration responseTimeout
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(connectTimeout.toMillis()))
                .responseTimeout(responseTimeout);
        return autoConfiguredWebClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((request, next) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId == null) {
                        return next.exchange(request);
                    }
                    return next.exchange(ClientRequest.from(request)
                            .header("X-Correlation-Id", correlationId)
                            .build());
                })
                .filter(loadBalancerFilter)
                .build();
    }
}
