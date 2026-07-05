package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final GetOnboardingApplicationUseCase getOnboardingApplicationUseCase;
    private final OnboardingWebMapper mapper;

    public OnboardingApplicationController(
            StartOnboardingApplicationUseCase startOnboardingApplicationUseCase,
            ConsumeMagicLinkUseCase consumeMagicLinkUseCase,
            ValidateContinuationUseCase validateContinuationUseCase,
            GetOnboardingApplicationUseCase getOnboardingApplicationUseCase,
            OnboardingWebMapper mapper
    ) {
        this.startOnboardingApplicationUseCase = startOnboardingApplicationUseCase;
        this.consumeMagicLinkUseCase = consumeMagicLinkUseCase;
        this.validateContinuationUseCase = validateContinuationUseCase;
        this.getOnboardingApplicationUseCase = getOnboardingApplicationUseCase;
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
    public OnboardingApplicationResponse validateContinuation(@Valid @RequestBody ValidateContinuationRequest request) {
        return mapper.toResponse(validateContinuationUseCase.validate(mapper.toCommand(request)));
    }
}
