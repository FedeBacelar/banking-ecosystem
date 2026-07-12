package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class HomeBankingExceptionHandlerTest {

    private final HomeBankingExceptionHandler handler = new HomeBankingExceptionHandler();

    @Test
    void shouldHideDownstreamAuthorizationDetails() {
        WebClientResponseException exception = WebClientResponseException.Forbidden.create(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        ProblemDetail problem = handler.handleDownstreamError(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getProperties())
                .containsEntry("code", "ONBOARDING_SERVICE_UNAVAILABLE")
                .doesNotContainKey("downstreamStatus");
    }

    @Test
    void shouldPreserveOnlyAllowlistedPublicOnboardingCode() {
        WebClientResponseException exception = WebClientResponseException.UnprocessableEntity.create(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable entity",
                HttpHeaders.EMPTY,
                """
                {
                  "title": "Onboarding incomplete",
                  "code": "ONBOARDING_INCOMPLETE"
                }
                """.getBytes(),
                null
        );

        ProblemDetail problem = handler.handleDownstreamError(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getProperties())
                .containsEntry("code", "ONBOARDING_INCOMPLETE")
                .doesNotContainKey("downstreamStatus");
    }

    @Test
    void shouldPreserveTheStableDocumentStorageFailureCode() {
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service unavailable",
                HttpHeaders.EMPTY,
                """
                {
                  "code": "ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE"
                }
                """.getBytes(),
                null
        );

        ProblemDetail problem = handler.handleDownstreamError(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getProperties()).containsEntry("code", "ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE");
    }
}
