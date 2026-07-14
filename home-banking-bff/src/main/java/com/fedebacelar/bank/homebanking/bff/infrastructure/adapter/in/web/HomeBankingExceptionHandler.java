package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedebacelar.bank.homebanking.bff.application.exception.InternalAccessUnavailableException;
import com.fedebacelar.bank.homebanking.bff.application.exception.IdentityNotLinkedException;
import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import jakarta.validation.ConstraintViolationException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class HomeBankingExceptionHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern RETRY_AFTER_SECONDS = Pattern.compile("[1-9][0-9]*");
    private static final Map<String, HttpStatus> PUBLIC_ONBOARDING_ERRORS = Map.ofEntries(
            Map.entry("INVALID_MAGIC_LINK_TOKEN", HttpStatus.BAD_REQUEST),
            Map.entry("INVALID_CONTINUATION_TOKEN", HttpStatus.UNAUTHORIZED),
            Map.entry("ONBOARDING_MAGIC_LINK_EXPIRED", HttpStatus.GONE),
            Map.entry("ONBOARDING_CONTINUATION_EXPIRED", HttpStatus.UNAUTHORIZED),
            Map.entry("ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED", HttpStatus.CONFLICT),
            Map.entry("ONBOARDING_INCOMPLETE", HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry("INVALID_ONBOARDING_DOCUMENT", HttpStatus.BAD_REQUEST),
            Map.entry("INVALID_ONBOARDING_MULTIPART", HttpStatus.BAD_REQUEST),
            Map.entry("ONBOARDING_DOCUMENT_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE),
            Map.entry("ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry("CREDENTIAL_INVITATION_COOLDOWN", HttpStatus.TOO_MANY_REQUESTS),
            Map.entry("ONBOARDING_COMPLETION_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("IDEMPOTENCY_CONFLICT", HttpStatus.CONFLICT)
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

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingMultipartPart(MissingServletRequestPartException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid onboarding submission");
        problem.setDetail("The onboarding submission is missing required information.");
        problem.setProperty("code", "INVALID_ONBOARDING_MULTIPART");
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.PAYLOAD_TOO_LARGE);
        problem.setTitle("Onboarding document too large");
        problem.setDetail("Each document must be no larger than 10 MB.");
        problem.setProperty("code", "ONBOARDING_DOCUMENT_TOO_LARGE");
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("The submitted data is invalid.");
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(WebClientResponseException.class)
    ResponseEntity<ProblemDetail> handleDownstreamError(WebClientResponseException exception) {
        Optional<String> downstreamCode = downstreamCode(exception)
                .filter(PUBLIC_ONBOARDING_ERRORS::containsKey);
        HttpStatus publicStatus = downstreamCode
                .map(PUBLIC_ONBOARDING_ERRORS::get)
                .orElse(HttpStatus.SERVICE_UNAVAILABLE);
        ProblemDetail problem = ProblemDetail.forStatus(publicStatus);
        problem.setTitle("No pudimos completar la operación");
        problem.setDetail("Intentá nuevamente más tarde.");
        problem.setProperty("code", downstreamCode.orElse("ONBOARDING_SERVICE_UNAVAILABLE"));
        ResponseEntity.BodyBuilder response = ResponseEntity.status(publicStatus);
        if (downstreamCode.filter("CREDENTIAL_INVITATION_COOLDOWN"::equals).isPresent()) {
            sanitizedRetryAfter(exception).ifPresent(value -> response.header(HttpHeaders.RETRY_AFTER, value));
        }
        return response.body(problem);
    }

    @ExceptionHandler(WebClientRequestException.class)
    ProblemDetail handleDownstreamUnavailable(WebClientRequestException exception) {
        return serviceUnavailableProblem();
    }

    @ExceptionHandler(InternalAccessUnavailableException.class)
    ProblemDetail handleInternalAccessUnavailable(InternalAccessUnavailableException exception) {
        return serviceUnavailableProblem();
    }

    private ProblemDetail serviceUnavailableProblem() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("No pudimos completar la operación");
        problem.setDetail("Intentá nuevamente más tarde.");
        problem.setProperty("code", "ONBOARDING_SERVICE_UNAVAILABLE");
        return problem;
    }

    private Optional<String> sanitizedRetryAfter(WebClientResponseException exception) {
        return Optional.ofNullable(exception.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
                .map(String::trim)
                .filter(value -> RETRY_AFTER_SECONDS.matcher(value).matches());
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
