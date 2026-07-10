package com.fedebacelar.bank.customer.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.customer.domain.exception.CustomerDocumentNotFoundException;
import com.fedebacelar.bank.customer.domain.exception.CustomerEmailNotFoundException;
import com.fedebacelar.bank.customer.domain.exception.CustomerNumberNotFoundException;
import com.fedebacelar.bank.customer.domain.exception.CustomerNotFoundException;
import com.fedebacelar.bank.customer.domain.exception.DuplicateDocumentException;
import com.fedebacelar.bank.customer.domain.exception.InvalidCustomerStatusTransitionException;
import com.fedebacelar.bank.customer.domain.exception.IdempotencyConflictException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateDocumentException.class)
    ProblemDetail handleDuplicateDocument(DuplicateDocumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/duplicate-document"));
        problem.setTitle("Duplicate document");
        return problem;
    }

    @ExceptionHandler({CustomerNotFoundException.class, CustomerDocumentNotFoundException.class, CustomerEmailNotFoundException.class, CustomerNumberNotFoundException.class})
    ProblemDetail handleCustomerNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/customer-not-found"));
        problem.setTitle("Customer not found");
        return problem;
    }

    @ExceptionHandler(InvalidCustomerStatusTransitionException.class)
    ProblemDetail handleInvalidStatusTransition(InvalidCustomerStatusTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/invalid-customer-status-transition"));
        problem.setTitle("Invalid customer status transition");
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail handleIdempotencyConflict(IdempotencyConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/idempotency-conflict"));
        problem.setTitle("Idempotency conflict");
        problem.setProperty("code", "IDEMPOTENCY_CONFLICT");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The customer was modified by another request. Retry the operation with the latest customer state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-customer-update"));
        problem.setTitle("Concurrent customer update");
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
