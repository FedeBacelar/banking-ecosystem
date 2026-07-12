package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedebacelar.bank.homebanking.bff.application.exception.IdentityNotLinkedException;
import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class HomeBankingExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, HttpStatus> PUBLIC_ONBOARDING_ERRORS = Map.of(
            "INVALID_MAGIC_LINK_TOKEN", HttpStatus.BAD_REQUEST,
            "INVALID_CONTINUATION_TOKEN", HttpStatus.UNAUTHORIZED,
            "ONBOARDING_MAGIC_LINK_EXPIRED", HttpStatus.GONE,
            "ONBOARDING_CONTINUATION_EXPIRED", HttpStatus.UNAUTHORIZED,
            "ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED", HttpStatus.CONFLICT,
            "ONBOARDING_INCOMPLETE", HttpStatus.UNPROCESSABLE_ENTITY,
            "ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
            "CREDENTIAL_INVITATION_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS
    );

    @ExceptionHandler(IdentityNotLinkedException.class)
    ProblemDetail handleIdentityNotLinked(IdentityNotLinkedException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Identity is not linked");
        problem.setDetail("The authenticated identity is not linked to a banking customer.");
        problem.setProperty("code", "IDENTITY_NOT_LINKED");
        return problem;
    }

    @ExceptionHandler(OnboardingSessionRequiredException.class)
    ProblemDetail handleOnboardingSessionRequired(OnboardingSessionRequiredException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Onboarding session required");
        problem.setDetail("You need a valid onboarding session to continue.");
        problem.setProperty("code", "ONBOARDING_SESSION_REQUIRED");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationError(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("The submitted data is invalid.");
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMalformedRequest(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("The submitted data is invalid.");
        problem.setProperty("code", "INVALID_REQUEST_BODY");
        return problem;
    }

    @ExceptionHandler(WebClientResponseException.class)
    ProblemDetail handleDownstreamError(WebClientResponseException exception) {
        Optional<String> downstreamCode = downstreamCode(exception)
                .filter(PUBLIC_ONBOARDING_ERRORS::containsKey);
        HttpStatus publicStatus = downstreamCode
                .map(PUBLIC_ONBOARDING_ERRORS::get)
                .orElse(HttpStatus.SERVICE_UNAVAILABLE);
        ProblemDetail problem = ProblemDetail.forStatus(publicStatus);
        problem.setTitle("Request could not be completed");
        problem.setDetail("We could not complete the operation right now. Try again later.");
        problem.setProperty("code", downstreamCode.orElse("ONBOARDING_SERVICE_UNAVAILABLE"));
        return problem;
    }

    private Optional<String> downstreamCode(WebClientResponseException exception) {
        try {
            String body = exception.getResponseBodyAsString(StandardCharsets.UTF_8);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode code = OBJECT_MAPPER.readTree(body).path("code");
            if (code.isTextual() && !code.asText().isBlank()) {
                return Optional.of(code.asText());
            }
            return Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
