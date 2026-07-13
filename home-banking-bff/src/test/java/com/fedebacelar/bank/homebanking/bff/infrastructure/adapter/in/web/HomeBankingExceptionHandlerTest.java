package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    @Test
    void shouldPreserveTheStableInvalidDocumentCode() {
        WebClientResponseException exception = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                HttpHeaders.EMPTY,
                "{\"code\":\"INVALID_ONBOARDING_DOCUMENT\"}".getBytes(),
                null
        );

        ProblemDetail problem = handler.handleDownstreamError(exception);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_ONBOARDING_DOCUMENT");
    }

    @Test
    void shouldReturnAStableMissingMultipartContract() {
        ProblemDetail problem = handler.handleMissingMultipartPart(
                new MissingServletRequestPartException("dniBack")
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("The onboarding submission is missing required information.");
        assertThat(problem.getProperties()).containsEntry("code", "INVALID_ONBOARDING_MULTIPART");
    }

    @Test
    void shouldReturnAStablePayloadTooLargeContract() {
        ProblemDetail problem = handler.handleMaxUploadSizeExceeded(
                new MaxUploadSizeExceededException(10L * 1024L * 1024L)
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problem.getDetail()).isEqualTo("Each document must be no larger than 10 MB.");
        assertThat(problem.getProperties()).containsEntry("code", "ONBOARDING_DOCUMENT_TOO_LARGE");
    }
}
