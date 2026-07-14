package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingCompletionStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingCompletionStatusResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
public class OnboardingCompletionController {

    private final GetOnboardingCompletionStatusUseCase getOnboardingCompletionStatusUseCase;

    public OnboardingCompletionController(
            GetOnboardingCompletionStatusUseCase getOnboardingCompletionStatusUseCase
    ) {
        this.getOnboardingCompletionStatusUseCase = getOnboardingCompletionStatusUseCase;
    }

    @GetMapping("/completion-status")
    public ResponseEntity<OnboardingCompletionStatusResponse> completionStatus(
            @AuthenticationPrincipal OidcUser user
    ) {
        OnboardingCompletionStatusResponse response = OnboardingCompletionStatusResponse.from(
                getOnboardingCompletionStatusUseCase.getForKeycloakSubject(user.getSubject())
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
