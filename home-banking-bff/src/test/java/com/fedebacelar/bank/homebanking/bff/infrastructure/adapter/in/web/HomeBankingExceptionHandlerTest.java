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
    void shouldMapDownstreamForbiddenToProblemDetail() {
        WebClientResponseException exception = WebClientResponseException.Forbidden.create(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        ProblemDetail problem = handler.handleDownstreamForbidden(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Access denied");
        assertThat(problem.getProperties())
                .containsEntry("code", "DOWNSTREAM_ACCESS_DENIED")
                .containsEntry("downstreamStatus", HttpStatus.FORBIDDEN.value());
    }

    @Test
    void shouldPreserveSafeDownstreamProblemCode() {
        WebClientResponseException exception = WebClientResponseException.Conflict.create(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                HttpHeaders.EMPTY,
                """
                {
                  "title": "Duplicate onboarding application",
                  "code": "DUPLICATE_ACTIVE_ONBOARDING_APPLICATION"
                }
                """.getBytes(),
                null
        );

        ProblemDetail problem = handler.handleDownstreamError(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getProperties())
                .containsEntry("code", "DUPLICATE_ACTIVE_ONBOARDING_APPLICATION")
                .containsEntry("downstreamStatus", HttpStatus.CONFLICT.value());
    }
}
