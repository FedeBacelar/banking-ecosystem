package com.fedebacelar.bank.homebanking.bff.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.exception.InvalidOnboardingDocumentException;
import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.port.out.DocumentServicePort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplication;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocument;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingTermsAcceptance;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class OnboardingFlowServiceTest {

    private final OnboardingServicePort onboardingServicePort = mock(OnboardingServicePort.class);
    private final DocumentServicePort documentServicePort = mock(DocumentServicePort.class);
    private final GetInternalAccessTokenPort getInternalAccessTokenPort = mock(GetInternalAccessTokenPort.class);
    private final OnboardingFlowService service = new OnboardingFlowService(
            onboardingServicePort,
            documentServicePort,
            getInternalAccessTokenPort
    );

    @Test
    void shouldStartApplicationUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingApplication application = new OnboardingApplication(
                UUID.randomUUID(),
                "applicant@example.com",
                "EMAIL_VERIFICATION_PENDING",
                Instant.now().plusSeconds(1800),
                null,
                null,
                Instant.now().plusSeconds(86400),
                Instant.now(),
                Instant.now()
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.startApplication("applicant@example.com", accessToken)).thenReturn(application);

        OnboardingApplication result = service.startApplication("applicant@example.com");

        assertThat(result).isEqualTo(application);
    }

    @Test
    void shouldConsumeMagicLinkUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingContinuation continuation = new OnboardingContinuation(
                UUID.randomUUID(),
                "IN_PROGRESS",
                "continuation-token",
                Instant.now().plusSeconds(7200)
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.consumeMagicLink("magic-token", accessToken)).thenReturn(continuation);

        OnboardingContinuation result = service.consumeMagicLink("magic-token");

        assertThat(result).isEqualTo(continuation);
    }

    @Test
    void shouldReturnAnonymousSessionWhenContinuationCookieIsMissing() {
        OnboardingSession result = service.getSession(null);

        assertThat(result.active()).isFalse();
        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort);
    }

    @Test
    void shouldValidateContinuationUsingInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingSession session = OnboardingSession.active(
                UUID.randomUUID(),
                "IN_PROGRESS",
                Instant.now().plusSeconds(7200)
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.validateContinuation("continuation-token", accessToken)).thenReturn(session);

        OnboardingSession result = service.getSession("continuation-token");

        assertThat(result).isEqualTo(session);
        verify(onboardingServicePort).validateContinuation("continuation-token", accessToken);
    }

    @Test
    void shouldSaveApplicantDataUsingContinuationCookieAndInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingApplicantData applicantData = applicantData(null);
        OnboardingApplicantData savedData = applicantData(UUID.randomUUID());

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.saveApplicantData("continuation-token", applicantData, accessToken)).thenReturn(savedData);

        OnboardingApplicantData result = service.saveApplicantData("continuation-token", applicantData);

        assertThat(result).isEqualTo(savedData);
    }

    @Test
    void shouldUploadDocumentAndSaveReferenceUsingContinuationCookie() {
        String accessToken = "internal-token";
        UUID applicationId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "dni-front.png",
                "image/png",
                "content".getBytes()
        );
        OnboardingSession session = OnboardingSession.active(
                applicationId,
                "IN_PROGRESS",
                Instant.parse("2026-07-05T12:00:00Z")
        );
        OnboardingDocument document = new OnboardingDocument(
                documentId,
                "ONBOARDING_APPLICATION",
                applicationId.toString(),
                "DNI_FRONT",
                "dni-front.png",
                "image/png",
                7L,
                "STORED",
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:00Z")
        );
        OnboardingDocumentReference reference = new OnboardingDocumentReference(
                UUID.randomUUID(),
                applicationId,
                "DNI_FRONT",
                documentId,
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:00Z")
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.validateContinuation("continuation-token", accessToken)).thenReturn(session);
        when(documentServicePort.uploadOnboardingDocument(applicationId, "DNI_FRONT", file, accessToken)).thenReturn(document);
        when(onboardingServicePort.saveDocumentReference("continuation-token", "DNI_FRONT", documentId, accessToken))
                .thenReturn(reference);

        OnboardingDocumentReference result = service.uploadDocument("continuation-token", "DNI_FRONT", file);

        assertThat(result).isEqualTo(reference);
    }

    @Test
    void shouldRequireContinuationCookieBeforeUploadingDocument() {
        MockMultipartFile file = new MockMultipartFile("file", "dni-front.png", "image/png", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument(null, "DNI_FRONT", file))
                .isInstanceOf(OnboardingSessionRequiredException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort, documentServicePort);
    }

    @Test
    void shouldRejectUnsupportedDocumentCategoryBeforeCallingInternalServices() {
        MockMultipartFile file = new MockMultipartFile("file", "dni-front.png", "image/png", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument("continuation-token", "PAYSTUB", file))
                .isInstanceOf(InvalidOnboardingDocumentException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort, documentServicePort);
    }

    @Test
    void shouldRejectEmptyDocumentBeforeCallingInternalServices() {
        MockMultipartFile file = new MockMultipartFile("file", "dni-front.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadDocument("continuation-token", "DNI_FRONT", file))
                .isInstanceOf(InvalidOnboardingDocumentException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort, documentServicePort);
    }

    @Test
    void shouldRejectUnsupportedDocumentContentTypeBeforeCallingInternalServices() {
        MockMultipartFile file = new MockMultipartFile("file", "dni-front.txt", "text/plain", "content".getBytes());

        assertThatThrownBy(() -> service.uploadDocument("continuation-token", "DNI_FRONT", file))
                .isInstanceOf(InvalidOnboardingDocumentException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort, documentServicePort);
    }

    @Test
    void shouldAcceptTermsUsingContinuationCookieAndInternalAccessToken() {
        String accessToken = "internal-token";
        OnboardingTermsAcceptance acceptance = new OnboardingTermsAcceptance(
                UUID.randomUUID(),
                "ONBOARDING_TERMS_AR_V1",
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:00Z"),
                Instant.parse("2026-07-05T10:00:00Z")
        );

        when(getInternalAccessTokenPort.getAccessToken()).thenReturn(accessToken);
        when(onboardingServicePort.acceptTerms("continuation-token", true, "ONBOARDING_TERMS_AR_V1", accessToken))
                .thenReturn(acceptance);

        OnboardingTermsAcceptance result = service.acceptTerms("continuation-token", true, "ONBOARDING_TERMS_AR_V1");

        assertThat(result).isEqualTo(acceptance);
    }

    @Test
    void shouldRequireContinuationCookieBeforeAcceptingTerms() {
        assertThatThrownBy(() -> service.acceptTerms(null, true, "ONBOARDING_TERMS_AR_V1"))
                .isInstanceOf(OnboardingSessionRequiredException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort, documentServicePort);
    }

    @Test
    void shouldRequireContinuationCookieBeforeSavingApplicantData() {
        assertThatThrownBy(() -> service.saveApplicantData(null, applicantData(null)))
                .isInstanceOf(OnboardingSessionRequiredException.class);

        verifyNoInteractions(getInternalAccessTokenPort, onboardingServicePort);
    }

    private OnboardingApplicantData applicantData(UUID applicationId) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return new OnboardingApplicantData(
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
                applicationId == null ? null : now,
                applicationId == null ? null : now
        );
    }
}
