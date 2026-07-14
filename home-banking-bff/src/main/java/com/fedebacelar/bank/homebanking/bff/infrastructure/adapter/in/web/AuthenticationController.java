package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private static final String KEYCLOAK_AUTHORIZATION_PATH = "/oauth2/authorization/keycloak";
    private static final String KEYCLOAK_ONBOARDING_COMPLETION_AUTHORIZATION_PATH =
            "/oauth2/authorization/keycloak-onboarding-completion";

    @GetMapping("/login/home")
    public ResponseEntity<Void> loginHome(HttpServletRequest request) {
        URI authorizationEndpoint = URI.create(request.getContextPath() + KEYCLOAK_AUTHORIZATION_PATH);
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationEndpoint).build();
    }

    @GetMapping("/login/onboarding-completion")
    public ResponseEntity<Void> loginOnboardingCompletion(HttpServletRequest request) {
        URI authorizationEndpoint = URI.create(
                request.getContextPath() + KEYCLOAK_ONBOARDING_COMPLETION_AUTHORIZATION_PATH
        );
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationEndpoint).build();
    }
}
