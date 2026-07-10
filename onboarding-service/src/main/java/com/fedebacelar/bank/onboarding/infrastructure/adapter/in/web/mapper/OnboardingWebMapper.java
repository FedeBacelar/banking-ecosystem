package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper;

import com.fedebacelar.bank.onboarding.application.command.AcceptTermsCommand;
import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveApplicantDataCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveDocumentReferenceCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.view.ApplicantDataDetails;
import com.fedebacelar.bank.onboarding.application.view.DocumentReferenceDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.application.view.TermsAcceptanceDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.AcceptTermsRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ApplicantDataResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ConsumeMagicLinkRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.DocumentReferenceResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingApplicationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.OnboardingContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SaveApplicantDataRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.SaveDocumentReferenceRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.StartOnboardingApplicationRequest;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.TermsAcceptanceResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationResponse;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.dto.ValidateContinuationRequest;
import org.springframework.stereotype.Component;

@Component
public class OnboardingWebMapper {

    public StartOnboardingApplicationCommand toCommand(StartOnboardingApplicationRequest request) {
        return new StartOnboardingApplicationCommand(request.email());
    }

    public ConsumeMagicLinkCommand toCommand(ConsumeMagicLinkRequest request) {
        return new ConsumeMagicLinkCommand(request.token());
    }

    public ValidateContinuationCommand toCommand(ValidateContinuationRequest request) {
        return new ValidateContinuationCommand(request.token());
    }

    public SaveApplicantDataCommand toCommand(SaveApplicantDataRequest request) {
        return new SaveApplicantDataCommand(
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
                request.country()
        );
    }

    public SaveDocumentReferenceCommand toCommand(OnboardingDocumentCategory category, SaveDocumentReferenceRequest request) {
        return new SaveDocumentReferenceCommand(
                request.continuationToken(),
                category,
                request.documentId()
        );
    }

    public AcceptTermsCommand toCommand(AcceptTermsRequest request) {
        return new AcceptTermsCommand(
                request.continuationToken(),
                request.accepted(),
                request.termsVersion()
        );
    }

    public OnboardingApplicationResponse toResponse(OnboardingApplicationDetails details) {
        return new OnboardingApplicationResponse(
                details.id(),
                details.email(),
                details.status(),
                details.magicLinkExpiresAt(),
                details.magicLinkConsumedAt(),
                details.emailVerifiedAt(),
                details.continuationExpiresAt(),
                details.expiresAt(),
                details.createdAt(),
                details.updatedAt()
        );
    }

    public OnboardingContinuationResponse toResponse(OnboardingContinuationDetails details) {
        return new OnboardingContinuationResponse(
                details.applicationId(),
                details.email(),
                details.status(),
                details.continuationToken(),
                details.continuationExpiresAt()
        );
    }

    public ValidateContinuationResponse toValidateContinuationResponse(OnboardingApplicationDetails details) {
        return new ValidateContinuationResponse(
                details.id(),
                details.email(),
                details.status(),
                details.continuationExpiresAt()
        );
    }

    public ApplicantDataResponse toResponse(ApplicantDataDetails details) {
        return new ApplicantDataResponse(
                details.applicationId(),
                details.firstName(),
                details.middleName(),
                details.lastName(),
                details.birthDate(),
                details.nationality(),
                details.documentType(),
                details.documentNumber(),
                details.documentIssuingCountry(),
                details.documentExpirationDate(),
                details.phoneNumber(),
                details.street(),
                details.streetNumber(),
                details.city(),
                details.province(),
                details.postalCode(),
                details.country(),
                details.createdAt(),
                details.updatedAt()
        );
    }

    public DocumentReferenceResponse toResponse(DocumentReferenceDetails details) {
        return new DocumentReferenceResponse(
                details.id(),
                details.applicationId(),
                details.category(),
                details.documentId(),
                details.createdAt(),
                details.updatedAt()
        );
    }

    public TermsAcceptanceResponse toResponse(TermsAcceptanceDetails details) {
        return new TermsAcceptanceResponse(
                details.applicationId(),
                details.termsVersion(),
                details.acceptedAt(),
                details.createdAt(),
                details.updatedAt()
        );
    }
}
