package com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ResendCredentialInvitationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.RetryOnboardingWorkflowUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SubmitOnboardingUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingDocumentUploadException;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.error.GlobalExceptionHandler;
import com.fedebacelar.bank.onboarding.infrastructure.adapter.in.web.mapper.OnboardingWebMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
    private StartOnboardingApplicationUseCase startUseCase;
    @MockitoBean
    private ConsumeMagicLinkUseCase consumeUseCase;
    @MockitoBean
    private ValidateContinuationUseCase validateUseCase;
    @MockitoBean
    private GetOnboardingApplicationUseCase getUseCase;
    @MockitoBean
    private SubmitOnboardingUseCase submitUseCase;
    @MockitoBean
    private ResendCredentialInvitationUseCase resendUseCase;
    @MockitoBean
    private RetryOnboardingWorkflowUseCase retryUseCase;

    @Test
    void shouldStartApplication() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(startUseCase.start(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"person@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void shouldConsumeMagicLink() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(consumeUseCase.consume(any())).thenReturn(new OnboardingContinuationDetails(
                applicationId,
                "person@example.com",
                OnboardingApplicationStatus.IN_PROGRESS,
                "continuation-token",
                Instant.parse("2026-07-10T14:00:00Z")
        ));

        mockMvc.perform(post("/internal/onboarding/magic-links/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"magic-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.continuationToken").value("continuation-token"));
    }

    @Test
    void shouldValidateContinuationForBffStatusQueries() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(validateUseCase.validate(any())).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/continuations/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"continuation-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.email").value("person@example.com"));
    }

    @Test
    void shouldAcceptOneCompositeMultipartSubmission() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        when(submitUseCase.submit(any())).thenReturn(new OnboardingSubmissionDetails(
                applicationId, OnboardingApplicationStatus.SUBMITTED, now, now
        ));

        mockMvc.perform(multipart("/internal/onboarding/continuations/submissions")
                        .file(new MockMultipartFile(
                                "submission", "", MediaType.APPLICATION_JSON_VALUE,
                                validSubmissionJson().getBytes(StandardCharsets.UTF_8)
                        ))
                        .file(new MockMultipartFile("dniFront", "front.png", "image/png", new byte[]{1, 2}))
                        .file(new MockMultipartFile("dniBack", "back.png", "image/png", new byte[]{3, 4})))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void shouldRejectIncompleteCompositeSubmission() throws Exception {
        mockMvc.perform(multipart("/internal/onboarding/continuations/submissions")
                        .file(new MockMultipartFile(
                                "submission", "", MediaType.APPLICATION_JSON_VALUE,
                                validSubmissionJson().getBytes(StandardCharsets.UTF_8)
                        ))
                        .file(new MockMultipartFile("dniFront", "front.png", "image/png", new byte[]{1, 2})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ONBOARDING_MULTIPART"))
                .andExpect(jsonPath("$.detail").value("The onboarding submission is missing required information."));
    }

    @Test
    void shouldReturnAStableErrorWhenAnOnboardingDocumentIsInvalid() throws Exception {
        when(submitUseCase.submit(any())).thenThrow(new InvalidOnboardingDocumentException(
                new IllegalArgumentException("remote detail")
        ));

        mockMvc.perform(multipart("/internal/onboarding/continuations/submissions")
                        .file(new MockMultipartFile(
                                "submission", "", MediaType.APPLICATION_JSON_VALUE,
                                validSubmissionJson().getBytes(StandardCharsets.UTF_8)
                        ))
                        .file(new MockMultipartFile("dniFront", "front.png", "image/png", new byte[]{1, 2}))
                        .file(new MockMultipartFile("dniBack", "back.png", "image/png", new byte[]{3, 4})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ONBOARDING_DOCUMENT"))
                .andExpect(jsonPath("$.detail").value(
                        "The document must be a valid JPG, PNG, or PDF file of up to 10 MB."
                ));
    }

    @Test
    void shouldReturnAStableErrorWhenDocumentStorageIsUnavailable() throws Exception {
        when(submitUseCase.submit(any())).thenThrow(new OnboardingDocumentUploadException("internal detail"));

        mockMvc.perform(multipart("/internal/onboarding/continuations/submissions")
                        .file(new MockMultipartFile(
                                "submission", "", MediaType.APPLICATION_JSON_VALUE,
                                validSubmissionJson().getBytes(StandardCharsets.UTF_8)
                        ))
                        .file(new MockMultipartFile("dniFront", "front.png", "image/png", new byte[]{1, 2}))
                        .file(new MockMultipartFile("dniBack", "back.png", "image/png", new byte[]{3, 4})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ONBOARDING_DOCUMENT_UPLOAD_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Onboarding documents could not be stored right now."));
    }

    @Test
    void shouldResendCredentialInvitation() throws Exception {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        when(resendUseCase.resend("continuation-token", "request-12345678")).thenReturn(new OnboardingSubmissionDetails(
                UUID.randomUUID(), OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING, now, now
        ));

        mockMvc.perform(post("/internal/onboarding/continuations/credential-invitations/resend")
                        .header("Idempotency-Key", "request-12345678")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"continuationToken\":\"continuation-token\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CREDENTIAL_SETUP_PENDING"));
    }

    @Test
    void shouldRequireAnIdempotencyKeyForCredentialInvitationResend() throws Exception {
        mockMvc.perform(post("/internal/onboarding/continuations/credential-invitations/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"continuationToken\":\"continuation-token\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldKeepOperationalRetryEndpointsInternal() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(retryUseCase.retryReview(applicationId)).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(post("/internal/onboarding/applications/{applicationId}/review/retry", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    @Test
    void shouldNotExposeRemovedGranularCaptureEndpoints() throws Exception {
        mockMvc.perform(put("/internal/onboarding/continuations/applicant-data")).andExpect(status().isNotFound());
        mockMvc.perform(put("/internal/onboarding/continuations/documents/DNI_FRONT")).andExpect(status().isNotFound());
        mockMvc.perform(put("/internal/onboarding/continuations/terms")).andExpect(status().isNotFound());
    }

    @Test
    void shouldGetApplicationMetadataInternally() throws Exception {
        UUID applicationId = UUID.randomUUID();
        when(getUseCase.get(applicationId)).thenReturn(applicationDetails(applicationId));

        mockMvc.perform(get("/internal/onboarding/applications/{applicationId}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    private OnboardingApplicationDetails applicationDetails(UUID applicationId) {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
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

    private String validSubmissionJson() {
        return """
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
                  "country": "AR",
                  "termsAccepted": true
                }
                """;
    }
}
