package com.fedebacelar.bank.identity.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.identity.domain.exception.DuplicateIdentityLinkException;
import com.fedebacelar.bank.identity.domain.exception.IdentityLinkNotFoundException;
import com.fedebacelar.bank.identity.domain.exception.InactiveIdentityLinkException;
import com.fedebacelar.bank.identity.domain.exception.InvalidIdentityLinkStatusTransitionException;
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

    @ExceptionHandler(DuplicateIdentityLinkException.class)
    ProblemDetail handleDuplicateIdentityLink(DuplicateIdentityLinkException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/duplicate-identity-link"));
        problem.setTitle("Duplicate identity link");
        return problem;
    }

    @ExceptionHandler(IdentityLinkNotFoundException.class)
    ProblemDetail handleIdentityLinkNotFound(IdentityLinkNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/identity-link-not-found"));
        problem.setTitle("Identity link not found");
        return problem;
    }

    @ExceptionHandler({InactiveIdentityLinkException.class, InvalidIdentityLinkStatusTransitionException.class})
    ProblemDetail handleIdentityLinkConflict(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/identity-link-conflict"));
        problem.setTitle("Identity link conflict");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The identity link was modified by another request. Retry the operation with the latest identity link state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-identity-link-update"));
        problem.setTitle("Concurrent identity link update");
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-request"));
        problem.setTitle("Invalid request");
        problem.setDetail("Request body is malformed or contains invalid values");
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
}
