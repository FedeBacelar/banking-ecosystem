package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.port.in.GetOnboardingCompletionStatusUseCase;
import com.fedebacelar.bank.homebanking.bff.application.usecase.OnboardingFlowService;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionStatus;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingNextAction;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingPublicStatus;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.keycloak.client-id=home-banking-bff",
        "spring.security.oauth2.client.registration.keycloak.client-secret=local-bff-secret",
        "spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.keycloak.redirect-uri=http://localhost:8085/web/login/oauth2/code/keycloak",
        "spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email",
        "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/auth",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/token",
        "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/certs",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:8090/realms/banking-ecosystem/protocol/openid-connect/userinfo",
        "spring.security.oauth2.client.provider.keycloak.user-name-attribute=sub"
})
class OnboardingControllerTest {

    private static final String CONTINUATION_COOKIE = "NB_ONBOARDING_CONTINUATION";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingFlowService onboardingFlowService;

    @MockitoBean
    private GetAuthenticatedHomeContextUseCase getAuthenticatedHomeContextUseCase;

    @MockitoBean
    private GetOnboardingCompletionStatusUseCase getOnboardingCompletionStatusUseCase;

    @Test
    void shouldStartApplicationWithoutLoginAndReturnNoCustomerData() throws Exception {
        mockMvc.perform(post("/onboarding/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"applicant@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", "no-store, max-age=0"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$").doesNotExist());

        verify(onboardingFlowService).startApplication("applicant@example.com");
    }

    @Test
    void shouldRejectAnEmailThatExceedsTheInternalContractLimit() throws Exception {
        String oversizedEmail = "a".repeat(250) + "@example.com";

        mockMvc.perform(post("/onboarding/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + oversizedEmail + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldConsumeMagicLinkAndReturnOnlyRoutingInformation() throws Exception {
        Instant expiresAt = Instant.now().plusSeconds(7200);
        when(onboardingFlowService.consumeMagicLink("magic-token")).thenReturn(new OnboardingContinuation(
                UUID.randomUUID(),
                OnboardingState.IN_PROGRESS,
                "continuation-token",
                expiresAt
        ));

        mockMvc.perform(post("/onboarding/magic-links/consume")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"magic-token\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(CONTINUATION_COOKIE, "continuation-token"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.nextAction").value("CONTINUE_APPLICATION"))
                .andExpect(jsonPath("$.applicationId").doesNotExist())
                .andExpect(jsonPath("$.continuationExpiresAt").doesNotExist());
    }

    @Test
    void shouldSubmitApplicantDataTermsAndBothDocumentsAsOneRequest() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        when(onboardingFlowService.submit(eq("continuation-token"), any(), eq(true), any(), any()))
                .thenReturn(new OnboardingSubmission(applicationId, OnboardingState.SUBMITTED, now, now));

        mockMvc.perform(multipart("/onboarding/submissions")
                        .file(jsonPart("submission", validSubmissionJson()))
                        .file(filePart("dniFront", "dni-front.png"))
                        .file(filePart("dniBack", "dni-back.png"))
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void shouldReturnAStableErrorWhenMultipartSubmissionIsIncomplete() throws Exception {
        mockMvc.perform(multipart("/onboarding/submissions")
                        .file(jsonPart("submission", validSubmissionJson()))
                        .file(filePart("dniFront", "dni-front.png"))
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ONBOARDING_MULTIPART"));
    }

    @Test
    void shouldExposeOnlyPublicProcessStatus() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        when(onboardingFlowService.getStatus("continuation-token")).thenReturn(new OnboardingPublicStatus(
                applicationId,
                OnboardingState.PROVISIONING,
                OnboardingNextAction.WAIT,
                now
        ));

        mockMvc.perform(get("/onboarding/status")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("PROVISIONING"))
                .andExpect(jsonPath("$.nextAction").value("WAIT"))
                .andExpect(jsonPath("$.externalReference").doesNotExist())
                .andExpect(jsonPath("$.reasonCode").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void shouldResolveCompletionOnlyFromTheAuthenticatedOidcSubject() throws Exception {
        Instant updatedAt = Instant.parse("2026-07-13T12:00:00Z");
        when(getOnboardingCompletionStatusUseCase.getForKeycloakSubject("authenticated-subject"))
                .thenReturn(new OnboardingCompletionStatus(
                        OnboardingCompletionState.COMPLETED,
                        updatedAt
                ));

        mockMvc.perform(get("/onboarding/completion-status")
                        .param("keycloakSubject", "attacker-controlled-subject")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "unrelated-continuation"))
                        .with(oidcLogin().idToken(token -> token.subject("authenticated-subject"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.updatedAt").value(updatedAt.toString()))
                .andExpect(jsonPath("$.applicationId").doesNotExist())
                .andExpect(jsonPath("$.keycloakSubject").doesNotExist())
                .andExpect(jsonPath("$.subject").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());

        verify(getOnboardingCompletionStatusUseCase).getForKeycloakSubject("authenticated-subject");
    }

    @Test
    void shouldReturnAnApi401ForCompletionStatusWithoutAnOidcSession() throws Exception {
        mockMvc.perform(get("/onboarding/completion-status"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void shouldResendCredentialInvitationFromContinuationCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        when(onboardingFlowService.resendCredentialInvitation("continuation-token", "onboarding-resend-01"))
                .thenReturn(new OnboardingSubmission(
                        applicationId,
                        OnboardingState.CREDENTIAL_SETUP_PENDING,
                        now,
                        now
                ));

        mockMvc.perform(post("/onboarding/credential-invitations/resend")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .header("Idempotency-Key", "onboarding-resend-01")
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CREDENTIAL_SETUP_PENDING"));

        verify(onboardingFlowService).resendCredentialInvitation(
                "continuation-token",
                "onboarding-resend-01"
        );
    }

    @Test
    void shouldRequireAValidIdempotencyKeyForCredentialInvitationResend() throws Exception {
        mockMvc.perform(post("/onboarding/credential-invitations/resend")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .header("Idempotency-Key", "invalid key")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRequireCsrfForCookieAuthorizedMutations() throws Exception {
        mockMvc.perform(post("/onboarding/credential-invitations/resend")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void shouldNotExposeRemovedGranularWorkflowEndpoints() throws Exception {
        mockMvc.perform(get("/onboarding/session")).andExpect(status().isNotFound());
        mockMvc.perform(put("/onboarding/applicant-data").with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/onboarding/documents/DNI_FRONT").with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(put("/onboarding/terms").with(csrf())).andExpect(status().isNotFound());
    }

    private MockMultipartFile jsonPart(String name, String value) {
        return new MockMultipartFile(name, "", MediaType.APPLICATION_JSON_VALUE, value.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile filePart(String name, String filename) {
        return new MockMultipartFile(name, filename, MediaType.IMAGE_PNG_VALUE, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
    }

    private String validSubmissionJson() {
        return """
                {
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
