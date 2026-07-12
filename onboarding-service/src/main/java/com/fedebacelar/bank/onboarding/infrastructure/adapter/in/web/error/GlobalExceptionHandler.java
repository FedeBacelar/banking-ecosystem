package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidMagicLinkTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.NotificationDeliveryException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentUploadException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingIncompleteException;
import com.fedebacelar.bank.onboarding.domain.exception.CredentialInvitationCooldownException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
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
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "An onboarding application is already active for this email.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/duplicate-onboarding-application"));
        problem.setTitle("Duplicate onboarding application");
        problem.setProperty("code", "DUPLICATE_ACTIVE_ONBOARDING_APPLICATION");
        return problem;
    }

    @ExceptionHandler(OnboardingApplicationNotFoundException.class)
    ProblemDetail handleNotFound(OnboardingApplicationNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-application-not-found"));
        problem.setTitle("Onboarding application not found");
        problem.setProperty("code", "ONBOARDING_APPLICATION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(InvalidMagicLinkTokenException.class)
    ProblemDetail handleInvalidMagicLinkToken(InvalidMagicLinkTokenException exception) {
        return tokenProblem(HttpStatus.BAD_REQUEST, exception.getMessage(), "invalid-onboarding-token", "Invalid onboarding token", "INVALID_MAGIC_LINK_TOKEN");
    }

    @ExceptionHandler(InvalidContinuationTokenException.class)
    ProblemDetail handleInvalidContinuationToken(InvalidContinuationTokenException exception) {
        return tokenProblem(HttpStatus.BAD_REQUEST, exception.getMessage(), "invalid-onboarding-token", "Invalid onboarding token", "INVALID_CONTINUATION_TOKEN");
    }

    @ExceptionHandler(OnboardingMagicLinkExpiredException.class)
    ProblemDetail handleExpiredMagicLink(OnboardingMagicLinkExpiredException exception) {
        return tokenProblem(HttpStatus.GONE, exception.getMessage(), "expired-onboarding-token", "Expired onboarding token", "ONBOARDING_MAGIC_LINK_EXPIRED");
    }

    @ExceptionHandler(OnboardingContinuationExpiredException.class)
    ProblemDetail handleExpiredContinuation(OnboardingContinuationExpiredException exception) {
        return tokenProblem(HttpStatus.GONE, exception.getMessage(), "expired-onboarding-token", "Expired onboarding token", "ONBOARDING_CONTINUATION_EXPIRED");
    }

    @ExceptionHandler(OnboardingMagicLinkAlreadyConsumedException.class)
    ProblemDetail handleConsumedMagicLink(OnboardingMagicLinkAlreadyConsumedException exception) {
        return conflictProblem(exception.getMessage(), "ONBOARDING_MAGIC_LINK_ALREADY_CONSUMED");
    }

    @ExceptionHandler(InvalidOnboardingStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidOnboardingStatusTransitionException exception) {
        return conflictProblem(exception.getMessage(), "INVALID_ONBOARDING_STATUS_TRANSITION");
    }

    @ExceptionHandler(TermsAcceptanceRequiredException.class)
    ProblemDetail handleTermsAcceptanceRequired(TermsAcceptanceRequiredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/terms-acceptance-required"));
        problem.setTitle("Terms acceptance required");
        problem.setProperty("code", "TERMS_ACCEPTANCE_REQUIRED");
        return problem;
    }

    @ExceptionHandler(OnboardingIncompleteException.class)
    ProblemDetail handleIncompleteApplication(OnboardingIncompleteException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "The onboarding application is incomplete.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-incomplete"));
        problem.setTitle("Onboarding application incomplete");
        problem.setProperty("code", "ONBOARDING_INCOMPLETE");
        problem.setProperty("missingSections", exception.missingSections());
        return problem;
    }

    @ExceptionHandler(CredentialInvitationCooldownException.class)
    ProblemDetail handleCredentialInvitationCooldown(CredentialInvitationCooldownException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Please wait before requesting another invitation.");
        problem.setTitle("Credential invitation cooldown");
        problem.setProperty("code", "CREDENTIAL_INVITATION_COOLDOWN");
        return problem;
    }

    private ProblemDetail tokenProblem(HttpStatus status, String detail, String type, String title, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/" + type));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }

    private ProblemDetail conflictProblem(String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-conflict"));
        problem.setTitle("Onboarding conflict");
        problem.setProperty("code", code);
        return problem;
    }

    @ExceptionHandler(NotificationDeliveryException.class)
    ProblemDetail handleNotificationDelivery(NotificationDeliveryException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-notification-unavailable"));
        problem.setTitle("Onboarding notification unavailable");
        problem.setProperty("code", "ONBOARDING_NOTIFICATION_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(OnboardingDocumentUploadException.class)
    ProblemDetail handleDocumentUpload(OnboardingDocumentUploadException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Onboarding documents could not be stored right now."
        );
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/onboarding-document-storage-unavailable"));
        problem.setTitle("Onboarding document storage unavailable");
        problem.setProperty("code", "ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The onboarding application was modified by another request. Retry the operation with the latest application state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-onboarding-update"));
        problem.setTitle("Concurrent onboarding update");
        problem.setProperty("code", "CONCURRENT_ONBOARDING_UPDATE");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-request"));
        problem.setTitle("Invalid request");
        problem.setDetail("Request validation failed");
        problem.setProperty("code", "VALIDATION_ERROR");
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
        problem.setProperty("code", "VALIDATION_ERROR");
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
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }
}
