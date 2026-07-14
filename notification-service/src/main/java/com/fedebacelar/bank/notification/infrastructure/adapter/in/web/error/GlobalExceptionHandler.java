package com.fedebacelar.bank.notification.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.notification.domain.exception.InvalidTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.MissingTemplateVariableException;
import com.fedebacelar.bank.notification.domain.exception.NotificationRequestConflictException;
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

    @ExceptionHandler(MissingTemplateVariableException.class)
    ProblemDetail handleMissingTemplateVariable(MissingTemplateVariableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/missing-template-variable"));
        problem.setTitle("Missing template variable");
        return problem;
    }

    @ExceptionHandler(InvalidTemplateVariableException.class)
    ProblemDetail handleInvalidTemplateVariable(InvalidTemplateVariableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-template-variable"));
        problem.setTitle("Invalid template variable");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The notification was modified by another request. Retry the operation with the latest notification state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-notification-update"));
        problem.setTitle("Concurrent notification update");
        return problem;
    }

    @ExceptionHandler(NotificationRequestConflictException.class)
    ProblemDetail handleNotificationRequestConflict(NotificationRequestConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/notification-request-conflict"));
        problem.setTitle("Notification request conflict");
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

