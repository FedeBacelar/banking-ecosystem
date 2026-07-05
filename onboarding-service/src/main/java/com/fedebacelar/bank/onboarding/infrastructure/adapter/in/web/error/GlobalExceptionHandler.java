package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidMagicLinkTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.NotificationDeliveryException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateActiveOnboardingApplicationException.class)
    ProblemDetail handleDuplicate(DuplicateActiveOnboardingApplicationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/duplicate-onboarding-application"));
        problem.setTitle("Duplicate onboarding application");
        return problem;
    }

    @ExceptionHandler(OnboardingApplicationNotFoundException.class)
    ProblemDetail handleNotFound(OnboardingApplicationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-application-not-found"));
        problem.setTitle("Onboarding application not found");
        return problem;
    }

    @ExceptionHandler({InvalidMagicLinkTokenException.class, InvalidContinuationTokenException.class})
    ProblemDetail handleInvalidToken(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-onboarding-token"));
        problem.setTitle("Invalid onboarding token");
        return problem;
    }

    @ExceptionHandler({OnboardingMagicLinkExpiredException.class, OnboardingContinuationExpiredException.class})
    ProblemDetail handleExpiredToken(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/expired-onboarding-token"));
        problem.setTitle("Expired onboarding token");
        return problem;
    }

    @ExceptionHandler({OnboardingMagicLinkAlreadyConsumedException.class, InvalidOnboardingStatusTransitionException.class})
    ProblemDetail handleConflict(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-conflict"));
        problem.setTitle("Onboarding conflict");
        return problem;
    }

    @ExceptionHandler(NotificationDeliveryException.class)
    ProblemDetail handleNotificationDelivery(NotificationDeliveryException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-notification-unavailable"));
        problem.setTitle("Onboarding notification unavailable");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The onboarding application was modified by another request. Retry the operation with the latest application state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-onboarding-update"));
        problem.setTitle("Concurrent onboarding update");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-request"));
        problem.setTitle("Invalid request");
        problem.setDetail("Request validation failed");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-request"));
        problem.setTitle("Invalid request");
        problem.setDetail("Request validation failed");
        problem.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList());
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-request"));
        problem.setTitle("Invalid request");
        problem.setDetail("Request body is malformed or contains invalid values");
        return problem;
    }
}
