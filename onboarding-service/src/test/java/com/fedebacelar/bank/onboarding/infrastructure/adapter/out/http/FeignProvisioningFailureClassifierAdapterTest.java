package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.exception.ProvisioningRequestMismatchException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeignProvisioningFailureClassifierAdapterTest {
    private final FeignProvisioningFailureClassifierAdapter classifier =
            new FeignProvisioningFailureClassifierAdapter();

    @Test
    void retriesOnlyDeclaredTransientRemoteFailures() {
        assertThat(classifier.isRetryable(feignFailure(429))).isTrue();
        assertThat(classifier.isRetryable(feignFailure(503))).isTrue();
        assertThat(classifier.isRetryable(feignFailure(400))).isFalse();
        assertThat(classifier.isRetryable(
                new ProvisioningRequestMismatchException(ProvisioningStepType.CREATE_CUSTOMER)
        )).isFalse();
        assertThat(classifier.isRetryable(new IllegalStateException("broken invariant"))).isFalse();
    }

    private FeignException feignFailure(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://internal-service/resource",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null
        );
        Response response = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of())
                .build();
        return FeignException.errorStatus("InternalClient#call", response);
    }
}
