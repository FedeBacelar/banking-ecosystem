package com.fedebacelar.bank.homebanking.bff.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fedebacelar.bank.homebanking.bff.application.port.in.GetAuthenticatedHomeContextUseCase;
import com.fedebacelar.bank.homebanking.bff.application.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.usecase.OnboardingFlowService;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
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
        "spring.security.oauth2.client.registration.keycloak-service.provider=keycloak",
        "spring.security.oauth2.client.registration.keycloak-service.client-id=home-banking-bff",
        "spring.security.oauth2.client.registration.keycloak-service.client-secret=local-bff-secret",
        "spring.security.oauth2.client.registration.keycloak-service.authorization-grant-type=client_credentials",
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

    @Test
    void shouldStartOnboardingApplicationWithoutLogin() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(onboardingFlowService.startApplication("applicant@example.com"))
                .thenReturn(new OnboardingApplication(
                        applicationId,
                        "applicant@example.com",
                        "EMAIL_VERIFICATION_PENDING",
                        now.plusSeconds(1800),
                        null,
                        null,
                        now.plusSeconds(86400),
                        now,
                        now
                ));

        mockMvc.perform(post("/onboarding/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"applicant@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.email").value("applicant@example.com"))
                .andExpect(jsonPath("$.status").value("EMAIL_VERIFICATION_PENDING"));
    }

    @Test
    void shouldStartOnboardingApplicationThroughWebContextPathWithoutRedirectingToKeycloak() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(onboardingFlowService.startApplication("applicant@example.com"))
                .thenReturn(new OnboardingApplication(
                        applicationId,
                        "applicant@example.com",
                        "EMAIL_VERIFICATION_PENDING",
                        now.plusSeconds(1800),
                        null,
                        null,
                        now.plusSeconds(86400),
                        now,
                        now
                ));

        mockMvc.perform(post("/web/onboarding/applications")
                        .contextPath("/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"applicant@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(applicationId.toString()));
    }

    @Test
    void shouldConsumeMagicLinkAndSetContinuationCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(7200);

        when(onboardingFlowService.consumeMagicLink("magic-token"))
                .thenReturn(new OnboardingContinuation(applicationId, "IN_PROGRESS", "continuation-token", expiresAt));

        mockMvc.perform(post("/onboarding/magic-links/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"magic-token\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().value(CONTINUATION_COOKIE, "continuation-token"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Lax")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/web/onboarding")))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldConsumeMagicLinkThroughWebContextPathWithoutRedirectingToKeycloak() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(7200);

        when(onboardingFlowService.consumeMagicLink("magic-token"))
                .thenReturn(new OnboardingContinuation(applicationId, "IN_PROGRESS", "continuation-token", expiresAt));

        mockMvc.perform(post("/web/onboarding/magic-links/consume")
                        .contextPath("/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"magic-token\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(cookie().value(CONTINUATION_COOKIE, "continuation-token"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldExposeAnonymousOnboardingSessionWhenCookieIsMissing() throws Exception {
        when(onboardingFlowService.getSession(null)).thenReturn(OnboardingSession.anonymous());

        mockMvc.perform(get("/onboarding/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldValidateOnboardingSessionFromCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(7200);

        when(onboardingFlowService.getSession("continuation-token"))
                .thenReturn(OnboardingSession.active(applicationId, "IN_PROGRESS", expiresAt));

        mockMvc.perform(get("/onboarding/session")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldSaveApplicantDataFromContinuationCookie() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(onboardingFlowService.saveApplicantData(org.mockito.ArgumentMatchers.eq("continuation-token"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OnboardingApplicantData(
                        applicationId,
                        "Federico",
                        null,
                        "Bacelar",
                        LocalDate.parse("1990-05-10"),
                        "AR",
                        "DNI",
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
                ));

        mockMvc.perform(put("/onboarding/applicant-data")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                  "country": "AR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.documentType").value("DNI"));
    }

    @Test
    void shouldSaveApplicantDataThroughWebContextPathWithoutLogin() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(onboardingFlowService.saveApplicantData(org.mockito.ArgumentMatchers.eq("continuation-token"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OnboardingApplicantData(
                        applicationId,
                        "Federico",
                        null,
                        "Bacelar",
                        LocalDate.parse("1990-05-10"),
                        "AR",
                        "DNI",
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
                ));

        mockMvc.perform(put("/web/onboarding/applicant-data")
                        .contextPath("/web")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                  "country": "AR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()));
    }

    @Test
    void shouldUploadOnboardingDocumentThroughWebContextPathWithoutLogin() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID documentReferenceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.png",
                "image/png",
                "content".getBytes()
        );

        when(onboardingFlowService.uploadDocument(
                org.mockito.ArgumentMatchers.eq("continuation-token"),
                org.mockito.ArgumentMatchers.eq("DNI_FRONT"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(new OnboardingDocumentReference(
                documentReferenceId,
                applicationId,
                "DNI_FRONT",
                documentId,
                now,
                now
        ));

        mockMvc.perform(multipart("/web/onboarding/documents/{category}", "DNI_FRONT")
                        .file(file)
                        .contextPath("/web")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(documentReferenceId.toString()))
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.category").value("DNI_FRONT"))
                .andExpect(jsonPath("$.documentId").value(documentId.toString()));
    }

    @Test
    void shouldNotRedirectOnboardingApplicantDataToKeycloakWhenContinuationCookieIsMissing() throws Exception {
        doThrow(new OnboardingSessionRequiredException())
                .when(onboardingFlowService)
                .saveApplicantData(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(put("/web/onboarding/applicant-data")
                        .contextPath("/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                  "country": "AR"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value("ONBOARDING_SESSION_REQUIRED"));
    }

    @Test
    void shouldReturnBadRequestForInvalidOnboardingDocumentWithoutRedirectingToKeycloak() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.txt",
                "text/plain",
                "content".getBytes()
        );

        doThrow(new InvalidOnboardingDocumentException("Unsupported document file type."))
                .when(onboardingFlowService)
                .uploadDocument(
                        org.mockito.ArgumentMatchers.eq("continuation-token"),
                        org.mockito.ArgumentMatchers.eq("DNI_FRONT"),
                        org.mockito.ArgumentMatchers.any()
                );

        mockMvc.perform(multipart("/web/onboarding/documents/{category}", "DNI_FRONT")
                        .file(file)
                        .contextPath("/web")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token")))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value("INVALID_ONBOARDING_DOCUMENT"));
    }

    @Test
    void shouldNotRedirectOnboardingDocumentUploadToKeycloakWhenContinuationCookieIsMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.png",
                "image/png",
                "content".getBytes()
        );

        doThrow(new OnboardingSessionRequiredException())
                .when(onboardingFlowService)
                .uploadDocument(
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.eq("DNI_FRONT"),
                        org.mockito.ArgumentMatchers.any()
                );

        mockMvc.perform(multipart("/web/onboarding/documents/{category}", "DNI_FRONT")
                        .file(file)
                        .contextPath("/web"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value("ONBOARDING_SESSION_REQUIRED"));
    }

    @Test
    void shouldAcceptOnboardingTermsThroughWebContextPathWithoutLogin() throws Exception {
        UUID applicationId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-05T20:00:00Z");

        when(onboardingFlowService.acceptTerms("continuation-token", true, "ONBOARDING_TERMS_AR_V1"))
                .thenReturn(new OnboardingTermsAcceptance(
                        applicationId,
                        "ONBOARDING_TERMS_AR_V1",
                        now,
                        now,
                        now
                ));

        mockMvc.perform(put("/web/onboarding/terms")
                        .contextPath("/web")
                        .cookie(new Cookie(CONTINUATION_COOKIE, "continuation-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accepted": true,
                                  "termsVersion": "ONBOARDING_TERMS_AR_V1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationId").value(applicationId.toString()))
                .andExpect(jsonPath("$.termsVersion").value("ONBOARDING_TERMS_AR_V1"));
    }

    @Test
    void shouldNotRedirectOnboardingTermsToKeycloakWhenContinuationCookieIsMissing() throws Exception {
        doThrow(new OnboardingSessionRequiredException())
                .when(onboardingFlowService)
                .acceptTerms(null, true, "ONBOARDING_TERMS_AR_V1");

        mockMvc.perform(put("/web/onboarding/terms")
                        .contextPath("/web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accepted": true,
                                  "termsVersion": "ONBOARDING_TERMS_AR_V1"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.code").value("ONBOARDING_SESSION_REQUIRED"));
    }

    @Test
    void shouldClearOnboardingSessionCookie() throws Exception {
        mockMvc.perform(delete("/onboarding/session"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(CONTINUATION_COOKIE, 0))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/web/onboarding")));
    }
}
