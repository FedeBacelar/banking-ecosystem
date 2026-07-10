package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import com.fedebacelar.bank.homebanking.bff.application.port.in.AcceptOnboardingTermsUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.ConsumeOnboardingMagicLinkUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingSessionUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.SaveOnboardingApplicantDataUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.UploadOnboardingDocumentUseCase;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.AcceptOnboardingTermsRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.ConsumeOnboardingMagicLinkRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingApplicantDataResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingDocumentReferenceResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingSessionResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.OnboardingTermsAcceptanceResponse;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.SaveOnboardingApplicantDataRequest;
import com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web.dto.StartOnboardingRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {

    private static final String CONTINUATION_COOKIE_NAME = "NB_ONBOARDING_CONTINUATION";

    private final StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;
    private final ConsumeOnboardingMagicLinkUseCase consumeOnboardingMagicLinkUseCase;
    private final GetOnboardingSessionUseCase getOnboardingSessionUseCase;
    private final SaveOnboardingApplicantDataUseCase saveOnboardingApplicantDataUseCase;
    private final UploadOnboardingDocumentUseCase uploadOnboardingDocumentUseCase;
    private final AcceptOnboardingTermsUseCase acceptOnboardingTermsUseCase;
    private final boolean cookieSecure;

    public OnboardingController(
            StartOnboardingApplicationUseCase startOnboardingApplicationUseCase,
            ConsumeOnboardingMagicLinkUseCase consumeOnboardingMagicLinkUseCase,
            GetOnboardingSessionUseCase getOnboardingSessionUseCase,
            SaveOnboardingApplicantDataUseCase saveOnboardingApplicantDataUseCase,
            UploadOnboardingDocumentUseCase uploadOnboardingDocumentUseCase,
            AcceptOnboardingTermsUseCase acceptOnboardingTermsUseCase,
            @Value("${home-banking-bff.onboarding.cookie.secure:false}") boolean cookieSecure
    ) {
        this.startOnboardingApplicationUseCase = startOnboardingApplicationUseCase;
        this.consumeOnboardingMagicLinkUseCase = consumeOnboardingMagicLinkUseCase;
        this.getOnboardingSessionUseCase = getOnboardingSessionUseCase;
        this.saveOnboardingApplicantDataUseCase = saveOnboardingApplicantDataUseCase;
        this.uploadOnboardingDocumentUseCase = uploadOnboardingDocumentUseCase;
        this.acceptOnboardingTermsUseCase = acceptOnboardingTermsUseCase;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/applications")
    public ResponseEntity<OnboardingApplicationResponse> startApplication(
            @Valid @RequestBody StartOnboardingRequest request
    ) {
        OnboardingApplication application = startOnboardingApplicationUseCase.startApplication(request.email());
        return ResponseEntity
                .status(201)
                .body(OnboardingApplicationResponse.from(application));
    }

    @PostMapping("/magic-links/consume")
    public OnboardingSessionResponse consumeMagicLink(
            @Valid @RequestBody ConsumeOnboardingMagicLinkRequest request,
            HttpServletResponse response
    ) {
        OnboardingContinuation continuation = consumeOnboardingMagicLinkUseCase.consumeMagicLink(request.token());
        response.addHeader(HttpHeaders.SET_COOKIE, continuationCookie(continuation).toString());

        return OnboardingSessionResponse.from(OnboardingSession.active(
                continuation.applicationId(),
                continuation.status(),
                continuation.continuationExpiresAt()
        ));
    }

    @GetMapping("/session")
    public OnboardingSessionResponse session(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken
    ) {
        return OnboardingSessionResponse.from(getOnboardingSessionUseCase.getSession(continuationToken));
    }

    @PutMapping("/applicant-data")
    public OnboardingApplicantDataResponse saveApplicantData(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken,
            @Valid @RequestBody SaveOnboardingApplicantDataRequest request
    ) {
        OnboardingApplicantData applicantData = saveOnboardingApplicantDataUseCase.saveApplicantData(
                continuationToken,
                new OnboardingApplicantData(
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
                )
        );
        return OnboardingApplicantDataResponse.from(applicantData);
    }

    @PostMapping(value = "/documents/{category}", consumes = "multipart/form-data")
    public OnboardingDocumentReferenceResponse uploadDocument(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken,
            @PathVariable String category,
            @RequestPart("file") MultipartFile file
    ) {
        OnboardingDocumentReference reference = uploadOnboardingDocumentUseCase.uploadDocument(
                continuationToken,
                category,
                file
        );
        return OnboardingDocumentReferenceResponse.from(reference);
    }

    @PutMapping("/terms")
    public OnboardingTermsAcceptanceResponse acceptTerms(
            @CookieValue(name = CONTINUATION_COOKIE_NAME, required = false) String continuationToken,
            @Valid @RequestBody AcceptOnboardingTermsRequest request
    ) {
        OnboardingTermsAcceptance acceptance = acceptOnboardingTermsUseCase.acceptTerms(
                continuationToken,
                request.accepted(),
                request.termsVersion()
        );
        return OnboardingTermsAcceptanceResponse.from(acceptance);
    }

    @DeleteMapping("/session")
    public ResponseEntity<Void> clearSession(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, expiredContinuationCookie().toString());
        return ResponseEntity.noContent().build();
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

    private ResponseCookie expiredContinuationCookie() {
        return ResponseCookie.from(CONTINUATION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/web/onboarding")
                .maxAge(Duration.ZERO)
                .build();
    }

    private Duration cookieMaxAge(Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        if (maxAge.isNegative() || maxAge.isZero()) {
            return Duration.ZERO;
        }
        return maxAge;
    }
}
