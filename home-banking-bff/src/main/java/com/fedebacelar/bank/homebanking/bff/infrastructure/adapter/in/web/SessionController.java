package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.AuthenticatedUserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    public SessionController(GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase) {
        this.getAuthenticatedHomeContextUseCase = getAuthenticatedHomeContextUseCase;
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(@AuthenticationPrincipal OidcUser user, CsrfToken csrfToken) {
        HomeBankingContext context = getAuthenticatedHomeContextUseCase.getHomeContext(
                new AuthenticatedUser(user.getSubject(), user.getPreferredUsername(), user.getEmail())
        );

        // Loading the deferred token writes the readable XSRF cookie for top-level logout forms.
        csrfToken.getToken();
        return new AuthenticatedUserResponse(context.username(), displayName(user, context.username()));
    }

    private String displayName(OidcUser user, String fallback) {
        String fullName = user.getFullName();
        return fullName == null || fullName.isBlank() ? fallback : fullName;
    }
}
