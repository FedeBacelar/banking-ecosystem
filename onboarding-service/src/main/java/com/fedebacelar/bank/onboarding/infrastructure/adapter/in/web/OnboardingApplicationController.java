package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingCompletionStatusUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.RetryOnboardingWorkflowUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ContinuationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingCompletionStatusRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingCompletionStatusResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingSubmissionResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SubmitOnboardingRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/onboarding")
public class OnboardingApplicationController {

    private final StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;
    private final ConsumeMagicLinkUseCase consumeMagicLinkUseCase;
    private final ValidateContinuationUseCase validateContinuationUseCase;
    private final GetOnboardingApplicationUseCase getOnboardingApplicationUseCase;
    private final GetOnboardingCompletionStatusUseCase getOnboardingCompletionStatusUseCase;
    private final SubmitOnboardingUseCase submitOnboardingUseCase;
    private final ResendCredentialInvitationUseCase resendCredentialInvitationUseCase;
    private final RetryOnboardingWorkflowUseCase retryWorkflowUseCase;
    private final OnboardingWebMapper mapper;

    public OnboardingApplicationController(
            StartOnboardingApplicationUseCase startOnboardingApplicationUseCase,
            ConsumeMagicLinkUseCase consumeMagicLinkUseCase,
            ValidateContinuationUseCase validateContinuationUseCase,
            GetOnboardingApplicationUseCase getOnboardingApplicationUseCase,
            GetOnboardingCompletionStatusUseCase getOnboardingCompletionStatusUseCase,
            SubmitOnboardingUseCase submitOnboardingUseCase,
            ResendCredentialInvitationUseCase resendCredentialInvitationUseCase,
            RetryOnboardingWorkflowUseCase retryWorkflowUseCase,
            OnboardingWebMapper mapper
    ) {
        this.startOnboardingApplicationUseCase = startOnboardingApplicationUseCase;
        this.consumeMagicLinkUseCase = consumeMagicLinkUseCase;
        this.validateContinuationUseCase = validateContinuationUseCase;
        this.getOnboardingApplicationUseCase = getOnboardingApplicationUseCase;
        this.getOnboardingCompletionStatusUseCase = getOnboardingCompletionStatusUseCase;
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

    @Operation(summary = "Resolve onboarding completion status from an authenticated Keycloak subject")
    @PostMapping("/completion-status")
    public OnboardingCompletionStatusResponse completionStatus(
            @Valid @RequestBody OnboardingCompletionStatusRequest request
    ) {
        return OnboardingCompletionStatusResponse.from(
                getOnboardingCompletionStatusUseCase.getByKeycloakSubject(request.keycloakSubject())
        );
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

    @Operation(summary = "Submit the complete onboarding application")
    @PostMapping(value = "/continuations/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OnboardingSubmissionResponse submit(
            @Valid @RequestPart("submission") SubmitOnboardingRequest request,
            @RequestPart("dniFront") MultipartFile dniFront,
            @RequestPart("dniBack") MultipartFile dniBack
    ) {
        return OnboardingSubmissionResponse.from(submitOnboardingUseCase.submit(new SubmitOnboardingCommand(
                request.continuationToken(),
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
                request.termsAccepted(),
                MultipartDocumentUpload.from(dniFront),
                MultipartDocumentUpload.from(dniBack)
        )));
    }

    @Operation(summary = "Resend credential setup invitation")
    @PostMapping("/continuations/credential-invitations/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OnboardingSubmissionResponse resendCredentialInvitation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ContinuationRequest request
    ) {
        return OnboardingSubmissionResponse.from(
                resendCredentialInvitationUseCase.resend(request.continuationToken(), idempotencyKey)
        );
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
