package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingAccessResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.ConsumeOnboardingMagicLinkRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingSubmissionResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingStatusResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.StartOnboardingRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.SubmitOnboardingApplicationRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/onboarding")
@Validated
public class OnboardingController {

    private static final String CONTINUATION_COOKIE_NAME = "NB_ONBOARDING_CONTINUATION";

    private final StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;
    private final ConsumeOnboardingMagicLinkUseCase consumeOnboardingMagicLinkUseCase;
    private final SubmitOnboardingUseCase submitOnboardingUseCase;
    private final GetOnboardingStatusUseCase getOnboardingStatusUseCase;
    private final ResendCredentialInvitationUseCase resendCredentialInvitationUseCase;
    private final boolean cookieSecure;

    public OnboardingController(
            StartOnboardingApplicationUseCase startOnboardingApplicationUseCase,
            ConsumeOnboardingMagicLinkUseCase consumeOnboardingMagicLinkUseCase,
            SubmitOnboardingUseCase submitOnboardingUseCase,
            GetOnboardingStatusUseCase getOnboardingStatusUseCase,
            ResendCredentialInvitationUseCase resendCredentialInvitationUseCase,
            @Value("${home-banking-bff.onboarding.cookie.secure:false}") boolean cookieSecure
    ) {
        this.startOnboardingApplicationUseCase = startOnboardingApplicationUseCase;
        this.consumeOnboardingMagicLinkUseCase = consumeOnboardingMagicLinkUseCase;
        this.submitOnboardingUseCase = submitOnboardingUseCase;
        this.getOnboardingStatusUseCase = getOnboardingStatusUseCase;
        this.resendCredentialInvitationUseCase = resendCredentialInvitationUseCase;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/applications")
    public ResponseEntity<Void> startApplication(@Valid @RequestBody StartOnboardingRequest request) {
        startOnboardingApplicationUseCase.startApplication(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/magic-links/consume")
    public OnboardingAccessResponse consumeMagicLink(
            @Valid @RequestBody ConsumeOnboardingMagicLinkRequest request,
            HttpServletResponse response,
            CsrfToken csrfToken
    ) {
        OnboardingContinuation continuation = consumeOnboardingMagicLinkUseCase.consumeMagicLink(request.token());
        response.addHeader(HttpHeaders.SET_COOKIE, continuationCookie(continuation).toString());

        // Materializing the deferred token writes the readable XSRF cookie in this same response.
        csrfToken.getToken();
        return OnboardingAccessResponse.from(continuation.status());
    }

    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OnboardingSubmissionResponse> submit(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken,
            @Valid @RequestPart("submission") SubmitOnboardingApplicationRequest request,
            @RequestPart("dniFront") MultipartFile dniFront,
            @RequestPart("dniBack") MultipartFile dniBack
    ) {
        return ResponseEntity.accepted().body(OnboardingSubmissionResponse.from(
                submitOnboardingUseCase.submit(
                        continuationToken,
                        applicantData(request),
                        request.termsAccepted(),
                        new MultipartOnboardingFile(dniFront),
                        new MultipartOnboardingFile(dniBack)
                )
        ));
    }

    @GetMapping("/status")
    public OnboardingStatusResponse status(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken
    ) {
        return OnboardingStatusResponse.from(getOnboardingStatusUseCase.getStatus(continuationToken));
    }

    @PostMapping("/credential-invitations/resend")
    public ResponseEntity<OnboardingSubmissionResponse> resendCredentialInvitation(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken,
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(max = 128)
            @Pattern(regexp = "^[A-Za-z0-9:._-]+$")
            String idempotencyKey
    ) {
        return ResponseEntity.accepted().body(OnboardingSubmissionResponse.from(
                resendCredentialInvitationUseCase.resendCredentialInvitation(continuationToken, idempotencyKey)
        ));
    }

    private OnboardingApplicantData applicantData(SubmitOnboardingApplicationRequest request) {
        return new OnboardingApplicantData(
                null,
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.birthDate(),
                request.nationality(),
                request.documentType(),
                request.documentNumber(),
                request.documentIssuingCountry(),
                request.documentExpirationDate(),
                request.phoneNumber(),
                request.street(),
                request.streetNumber(),
                request.city(),
                request.province(),
                request.postalCode(),
                request.country(),
                null,
                null
        );
    }

    private ResponseCookie continuationCookie(OnboardingContinuation continuation) {
        return ResponseCookie.from(CONTINUATION_COOKIE_NAME, continuation.continuationToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/web/onboarding")
                .maxAge(cookieMaxAge(continuation.continuationExpiresAt()))
                .build();
    }

    private Duration cookieMaxAge(Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        return maxAge.isNegative() || maxAge.isZero() ? Duration.ZERO : maxAge;
    }
}
