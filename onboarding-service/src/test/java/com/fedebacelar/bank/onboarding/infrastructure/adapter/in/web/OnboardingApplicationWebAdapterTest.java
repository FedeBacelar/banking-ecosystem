package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.AcceptTermsUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveApplicantDataUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveDocumentReferenceUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.RetryOnboardingWorkflowUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.view.ApplicantDataDetails;
import com.fedebacelar.bank.onboarding.application.view.DocumentReferenceDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.application.view.TermsAcceptanceDetails;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = OnboardingApplicationController.class,
        excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import({OnboardingWebMapper.class, GlobalExceptionHandler.class})
class OnboardingApplicationWebAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartOnboardingApplicationUseCase startOnboardingApplicationUseCase;

    @MockitoBean
    private ConsumeMagicLinkUseCase consumeMagicLinkUseCase;

    @MockitoBean
    private ValidateContinuationUseCase validateContinuationUseCase;

    @MockitoBean
    private SaveApplicantDataUseCase saveApplicantDataUseCase;

    @MockitoBean
    private SaveDocumentReferenceUseCase saveDocumentReferenceUseCase;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private GetOnboardingApplicationUseCase getOnboardingApplicationUseCase;

    @MockitoBean
    private SubmitOnboardingUseCase submitOnboardingUseCase;

    @MockitoBean
    private ResendCredentialInvitationUseCase resendCredentialInvitationUseCase;

    @MockitoBean
    private RetryOnboardingWorkflowUseCase retryOnboardingWorkflowUseCase;

    @Test
    void startsApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(startOnboardingApplicationUseCase.start(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void returnsBadRequestForInvalidEmail() throws Exception {
        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void returnsConflictForDuplicateApplication() throws Exception {
        when(startOnboardingApplicationUseCase.start(any()))
                .thenThrow(new DuplicateActiveOnboardingApplicationException("person@example.com"));

        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "person@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate onboarding application"));
    }

    @Test
    void consumesMagicLink() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(consumeMagicLinkUseCase.consume(any())).thenReturn(new OnboardingContinuationDetails(
                applicationId,
                "person@example.com",
                OnboardingApplicationStatus.IN_PROGRESS,
                "continuation-token",
                Instant.parse("2026-07-05T12:00:00Z")
        ));

        mockMvc.perform(post("/internal/onboarding/magic-links/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "magic-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.continuationToken").value("continuation-token"));
    }

    @Test
    void validatesContinuation() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(validateContinuationUseCase.validate(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/continuations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "continuation-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.email").value("person@example.com"))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void savesApplicantData() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(saveApplicantDataUseCase.save(any())).thenReturn(applicantDataDetails(applicationId));

        mockMvc.perform(put("/internal/onboarding/continuations/applicant-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "continuationToken": "continuation-token",
                                  "firstName": "Federico",
                                  "lastName": "Bacelar",
                                  "birthDate": "1990-05-10",
                                  "nationality": "AR",
                                  "documentType": "DNI",
                                  "documentNumber": "12345678",
                                  "documentIssuingCountry": "AR",
                                  "phoneNumber": "+5491122223333",
                                  "street": "Av Siempre Viva",
                                  "streetNumber": "742",
                                  "city": "Buenos Aires",
                                  "province": "Buenos Aires",
                                  "postalCode": "1000",
                                  "country": "AR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.documentType").value("DNI"));
    }

    @Test
    void savesDocumentReference() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID documentReferenceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        when(saveDocumentReferenceUseCase.save(any())).thenReturn(new DocumentReferenceDetails(
                documentReferenceId,
                applicationId,
                OnboardingDocumentCategory.DNI_FRONT,
                documentId,
                now,
                now
        ));

        mockMvc.perform(put("/internal/onboarding/continuations/documents/DNI_FRONT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "continuationToken": "continuation-token",
                                  "documentId": "%s"
                                }
                                """.formatted(documentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentReferenceId.toString()))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.category").value("DNI_FRONT"))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()));
    }

    @Test
    void acceptsTerms() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        when(acceptTermsUseCase.accept(any())).thenReturn(new TermsAcceptanceDetails(
                applicationId,
                "ONBOARDING_TERMS_AR_V1",
                now,
                now,
                now
        ));

        mockMvc.perform(put("/internal/onboarding/continuations/terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "continuationToken": "continuation-token",
                                  "accepted": true,
                                  "termsVersion": "ONBOARDING_TERMS_AR_V1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.termsVersion").value("ONBOARDING_TERMS_AR_V1"));
    }

    @Test
    void getsApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(getOnboardingApplicationUseCase.get(applicationId)).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(get("/internal/onboarding/applications/{applicationId}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    private OnboardingApplicationDetails applicationDetails(UUID applicationId) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return new OnboardingApplicationDetails(
                applicationId,
                "person@example.com",
                OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
                now.plusSeconds(1800),
                null,
                null,
                null,
                now.plusSeconds(1296000),
                now,
                now
        );
    }

    private ApplicantDataDetails applicantDataDetails(UUID applicationId) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return new ApplicantDataDetails(
                applicationId,
                "Federico",
                null,
                "Bacelar",
                LocalDate.parse("1990-05-10"),
                "AR",
                ApplicantDocumentType.DNI,
                "12345678",
                "AR",
                null,
                "+5491122223333",
                "Av Siempre Viva",
                "742",
                "Buenos Aires",
                "Buenos Aires",
                "1000",
                "AR",
                now,
                now
        );
    }
}
