package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.exception.IdentityNotLinkedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class HomeBankingExceptionHandler {

    @ExceptionHandler(IdentityNotLinkedException.class)
    ProblemDetail handleIdentityNotLinked(IdentityNotLinkedException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Identity is not linked");
        problem.setDetail("The authenticated identity is not linked to a banking customer.");
        problem.setProperty("code", "IDENTITY_NOT_LINKED");
        problem.setProperty("providerSubject", exception.providerSubject());
        return problem;
    }

    @ExceptionHandler(WebClientResponseException.Forbidden.class)
    ProblemDetail handleDownstreamForbidden(WebClientResponseException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Access denied by internal service");
        problem.setDetail("The authenticated user does not have enough permissions to compose the home banking context.");
        problem.setProperty("code", "DOWNSTREAM_ACCESS_DENIED");
        problem.setProperty("downstreamStatus", exception.getStatusCode().value());
        return problem;
    }

    @ExceptionHandler(WebClientResponseException.class)
    ProblemDetail handleDownstreamError(WebClientResponseException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(exception.getStatusCode());
        problem.setTitle("Internal service request failed");
        problem.setDetail("An internal service rejected or failed a request needed to compose the home banking context.");
        problem.setProperty("code", "DOWNSTREAM_SERVICE_ERROR");
        problem.setProperty("downstreamStatus", exception.getStatusCode().value());
        return problem;
    }
}
