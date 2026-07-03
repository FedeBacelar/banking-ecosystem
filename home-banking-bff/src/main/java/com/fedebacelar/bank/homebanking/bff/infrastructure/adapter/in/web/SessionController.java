package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.AuthenticatedUser;
import com.fedebacelar.bank.homebanking.bff.domain.model.HomeBankingContext;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.AuthenticatedUserResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.SessionResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    public SessionController(GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase) {
        this.getAuthenticatedHomeContextUseCase = getAuthenticatedHomeContextUseCase;
    }

    @GetMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal OidcUser user) {
        if (user == null) {
            return new SessionResponse(false, null, null);
        }

        return new SessionResponse(true, user.getSubject(), user.getPreferredUsername());
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(
            @AuthenticationPrincipal OidcUser user,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient
    ) {
        HomeBankingContext context = getAuthenticatedHomeContextUseCase.getHomeContext(
                new AuthenticatedUser(user.getSubject(), user.getPreferredUsername(), user.getEmail()),
                authorizedClient.getAccessToken().getTokenValue()
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
