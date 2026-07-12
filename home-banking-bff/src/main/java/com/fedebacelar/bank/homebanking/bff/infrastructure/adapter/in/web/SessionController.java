package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.AuthenticatedUserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    public SessionController(GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase) {
        this.getAuthenticatedHomeContextUseCase = getAuthenticatedHomeContextUseCase;
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal OidcUser user) {
        HomeBankingContext context = getAuthenticatedHomeContextUseCase.getHomeContext(
                new AuthenticatedUser(user.getSubject(), user.getPreferredUsername(), user.getEmail())
        );

        return new AuthenticatedUserResponse(
                context.subject(),
                context.username(),
                context.email(),
                context.customerId(),
                context.customer(),
                context.accounts()
        );
    }
}
