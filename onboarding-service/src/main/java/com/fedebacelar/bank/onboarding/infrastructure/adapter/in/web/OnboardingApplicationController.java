package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import com.fedebacelar.bank.onboarding.application.port.in.AcceptTermsUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveApplicantDataUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveDocumentReferenceUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.RetryOnboardingWorkflowUseCase;
import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.AcceptTermsRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ApplicantDataResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.DocumentReferenceResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingSubmissionResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SaveApplicantDataRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SaveDocumentReferenceRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SubmitOnboardingRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.TermsAcceptanceResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/onboarding")
public class OnboardingApplicationController {

    private final StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;
    private final ConsumeMagicLinkUseCase consumeMagicLinkUseCase;
    private final ValidateContinuationUseCase validateContinuationUseCase;
    private final SaveApplicantDataUseCase saveApplicantDataUseCase;
    private final SaveDocumentReferenceUseCase saveDocumentReferenceUseCase;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final GetOnboardingApplicationUseCase getOnboardingApplicationUseCase;
    private final SubmitOnboardingUseCase submitOnboardingUseCase;
    private final ResendCredentialInvitationUseCase resendCredentialInvitationUseCase;
    private final RetryOnboardingWorkflowUseCase retryWorkflowUseCase;
    private final OnboardingWebMapper mapper;

    public OnboardingApplicationController(
            StartOnboardingApplicationUseCase startOnboardingApplicationUseCase,
            ConsumeMagicLinkUseCase consumeMagicLinkUseCase,
            ValidateContinuationUseCase validateContinuationUseCase,
            SaveApplicantDataUseCase saveApplicantDataUseCase,
            SaveDocumentReferenceUseCase saveDocumentReferenceUseCase,
            AcceptTermsUseCase acceptTermsUseCase,
            GetOnboardingApplicationUseCase getOnboardingApplicationUseCase,
            SubmitOnboardingUseCase submitOnboardingUseCase,
            ResendCredentialInvitationUseCase resendCredentialInvitationUseCase,
            RetryOnboardingWorkflowUseCase retryWorkflowUseCase,
            OnboardingWebMapper mapper
    ) {
        this.startOnboardingApplicationUseCase = startOnboardingApplicationUseCase;
        this.consumeMagicLinkUseCase = consumeMagicLinkUseCase;
        this.validateContinuationUseCase = validateContinuationUseCase;
        this.saveApplicantDataUseCase = saveApplicantDataUseCase;
        this.saveDocumentReferenceUseCase = saveDocumentReferenceUseCase;
        this.acceptTermsUseCase = acceptTermsUseCase;
        this.getOnboardingApplicationUseCase = getOnboardingApplicationUseCase;
        this.submitOnboardingUseCase = submitOnboardingUseCase;
        this.resendCredentialInvitationUseCase = resendCredentialInvitationUseCase;
        this.retryWorkflowUseCase = retryWorkflowUseCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Start onboarding application")
    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public OnboardingApplicationResponse start(@Valid @RequestBody StartOnboardingApplicationRequest request) {
        return mapper.toResponse(startOnboardingApplicationUseCase.start(mapper.toCommand(request)));
    }

    @Operation(summary = "Get onboarding application metadata")
    @GetMapping("/applications/{applicationId}")
    public OnboardingApplicationResponse get(@PathVariable UUID applicationId) {
        return mapper.toResponse(getOnboardingApplicationUseCase.get(applicationId));
    }

    @Operation(summary = "Consume onboarding magic link")
    @PostMapping("/magic-links/consume")
    public OnboardingContinuationResponse consumeMagicLink(@Valid @RequestBody ConsumeMagicLinkRequest request) {
        return mapper.toResponse(consumeMagicLinkUseCase.consume(mapper.toCommand(request)));
    }

    @Operation(summary = "Validate onboarding continuation token")
    @PostMapping("/continuations/validate")
    public ValidateContinuationResponse validateContinuation(@Valid @RequestBody ValidateContinuationRequest request) {
        return mapper.toValidateContinuationResponse(validateContinuationUseCase.validate(mapper.toCommand(request)));
    }

    @Operation(summary = "Save onboarding applicant data")
    @PutMapping("/continuations/applicant-data")
    public ApplicantDataResponse saveApplicantData(@Valid @RequestBody SaveApplicantDataRequest request) {
        return mapper.toResponse(saveApplicantDataUseCase.save(mapper.toCommand(request)));
    }

    @Operation(summary = "Save onboarding document reference")
    @PutMapping("/continuations/documents/{category}")
    public DocumentReferenceResponse saveDocumentReference(
            @PathVariable OnboardingDocumentCategory category,
            @Valid @RequestBody SaveDocumentReferenceRequest request
    ) {
        return mapper.toResponse(saveDocumentReferenceUseCase.save(mapper.toCommand(category, request)));
    }

    @Operation(summary = "Accept onboarding terms")
    @PutMapping("/continuations/terms")
    public TermsAcceptanceResponse acceptTerms(@Valid @RequestBody AcceptTermsRequest request) {
        return mapper.toResponse(acceptTermsUseCase.accept(mapper.toCommand(request)));
    }

    @Operation(summary = "Submit onboarding application")
    @PostMapping("/continuations/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OnboardingSubmissionResponse submit(@Valid @RequestBody SubmitOnboardingRequest request) {
        return OnboardingSubmissionResponse.from(submitOnboardingUseCase.submit(new SubmitOnboardingCommand(request.continuationToken())));
    }

    @Operation(summary = "Resend credential setup invitation")
    @PostMapping("/continuations/credential-invitations/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OnboardingSubmissionResponse resendCredentialInvitation(@Valid @RequestBody SubmitOnboardingRequest request) {
        return OnboardingSubmissionResponse.from(resendCredentialInvitationUseCase.resend(request.continuationToken()));
    }

    @Operation(summary = "Retry failed AUTO review")
    @PostMapping("/applications/{applicationId}/review/retry")
    public OnboardingApplicationResponse retryReview(@PathVariable UUID applicationId) {
        return mapper.toResponse(retryWorkflowUseCase.retryReview(applicationId));
    }

    @Operation(summary = "Retry failed provisioning")
    @PostMapping("/applications/{applicationId}/provisioning/retry")
    public OnboardingApplicationResponse retryProvisioning(@PathVariable UUID applicationId) {
        return mapper.toResponse(retryWorkflowUseCase.retryProvisioning(applicationId));
    }
}
