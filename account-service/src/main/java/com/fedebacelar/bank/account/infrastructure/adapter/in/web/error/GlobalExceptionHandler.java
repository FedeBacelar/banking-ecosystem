package com.fedebacelar.bank.account.infrastructure.adapter.in.web.error;

import com.fedebacelar.bank.account.domain.exception.AccountBalanceNotZeroException;
import com.fedebacelar.bank.account.domain.exception.AccountClosedException;
import com.fedebacelar.bank.account.domain.exception.AccountNotFoundException;
import com.fedebacelar.bank.account.domain.exception.AccountNumberNotFoundException;
import com.fedebacelar.bank.account.domain.exception.AliasNotFoundException;
import com.fedebacelar.bank.account.domain.exception.CustomerLookupException;
import com.fedebacelar.bank.account.domain.exception.CustomerNotEligibleForAccountException;
import com.fedebacelar.bank.account.domain.exception.DuplicateAccountAliasException;
import com.fedebacelar.bank.account.domain.exception.ExternalCustomerNotFoundException;
import com.fedebacelar.bank.account.domain.exception.InvalidAccountStatusTransitionException;
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

    @ExceptionHandler(DuplicateAccountAliasException.class)
    ProblemDetail handleDuplicateAlias(DuplicateAccountAliasException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/duplicate-account-alias"));
        problem.setTitle("Duplicate account alias");
        return problem;
    }

    @ExceptionHandler({AccountNotFoundException.class, AccountNumberNotFoundException.class, AliasNotFoundException.class})
    ProblemDetail handleAccountNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/account-not-found"));
        problem.setTitle("Account not found");
        return problem;
    }

    @ExceptionHandler(ExternalCustomerNotFoundException.class)
    ProblemDetail handleCustomerNotFound(ExternalCustomerNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/customer-not-found"));
        problem.setTitle("Customer not found");
        return problem;
    }

    @ExceptionHandler(CustomerLookupException.class)
    ProblemDetail handleCustomerLookup(CustomerLookupException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/customer-service-unavailable"));
        problem.setTitle("Customer service unavailable");
        return problem;
    }

    @ExceptionHandler({
            InvalidAccountStatusTransitionException.class,
            AccountBalanceNotZeroException.class,
            AccountClosedException.class,
            CustomerNotEligibleForAccountException.class
    })
    ProblemDetail handleAccountConflict(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/account-conflict"));
        problem.setTitle("Account conflict");
        return problem;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The account was modified by another request. Retry the operation with the latest account state.");
        problem.setType(URI.create("https://bank.fedebacelar.com/problems/concurrent-account-update"));
        problem.setTitle("Concurrent account update");
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
