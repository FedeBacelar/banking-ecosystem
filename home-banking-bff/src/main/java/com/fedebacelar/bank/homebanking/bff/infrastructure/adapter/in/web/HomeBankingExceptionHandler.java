package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedebacelar.bank.homebanking.bff.application.exception.IdentityNotLinkedException;
import com.fedebacelar.bank.homebanking.bff.application.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import java.nio.charset.StandardCharsets;
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

    @ExceptionHandler(IdentityNotLinkedException.class)
    ProblemDetail handleIdentityNotLinked(IdentityNotLinkedException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Identity is not linked");
        problem.setDetail("The authenticated identity is not linked to a banking customer.");
        problem.setProperty("code", "IDENTITY_NOT_LINKED");
        problem.setProperty("providerSubject", exception.providerSubject());
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

    @ExceptionHandler(InvalidOnboardingDocumentException.class)
    ProblemDetail handleInvalidOnboardingDocument(InvalidOnboardingDocumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid onboarding document");
        problem.setDetail("The uploaded document cannot be accepted.");
        problem.setProperty("code", "INVALID_ONBOARDING_DOCUMENT");
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

    @ExceptionHandler(WebClientResponseException.Forbidden.class)
    ProblemDetail handleDownstreamForbidden(WebClientResponseException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access denied");
        problem.setDetail("You do not have enough permissions to complete this operation.");
        problem.setProperty("code", "DOWNSTREAM_ACCESS_DENIED");
        problem.setProperty("downstreamStatus", exception.getStatusCode().value());
        return problem;
    }

    @ExceptionHandler(WebClientResponseException.class)
    ProblemDetail handleDownstreamError(WebClientResponseException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.getStatusCode());
        problem.setTitle("Request could not be completed");
        problem.setDetail("We could not complete the operation right now. Try again later.");
        problem.setProperty("code", downstreamCode(exception).orElse("DOWNSTREAM_SERVICE_ERROR"));
        problem.setProperty("downstreamStatus", exception.getStatusCode().value());
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
