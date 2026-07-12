package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.OnboardingDocumentUpload;
import com.fedebacelar.bank.onboarding.application.command.SubmitOnboardingCommand;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentUploadPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingSubmissionDetails;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitOnboardingApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");
    private static final String CONTINUATION_TOKEN = "continuation-token";

    private final OnboardingApplicationRepositoryPort applications = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingDocumentUploadPort documents = mock(OnboardingDocumentUploadPort.class);
    private final OnboardingSubmissionFinalizer finalizer = mock(OnboardingSubmissionFinalizer.class);
    private final TokenHashingPort hashing = mock(TokenHashingPort.class);
    private final SubmitOnboardingApplicationService service = new SubmitOnboardingApplicationService(
            applications,
            documents,
            finalizer,
            hashing,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @BeforeEach
    void setUp() {
        when(hashing.hash(CONTINUATION_TOKEN)).thenReturn("continuation-hash");
    }

    @Test
    void shouldUploadBothDocumentsIdempotentlyThenFinalizeTheCompositeSubmission() {
        OnboardingApplication application = inProgressApplication();
        SubmitOnboardingCommand command = command(true);
        UUID frontId = UUID.randomUUID();
        UUID backId = UUID.randomUUID();
        OnboardingSubmissionDetails expected = new OnboardingSubmissionDetails(
                application.id(), OnboardingApplicationStatus.SUBMITTED, NOW, NOW
        );
        when(applications.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(documents.upload(eq(application.id()), eq(OnboardingDocumentCategory.DNI_FRONT), eq(command.dniFront()), any()))
                .thenReturn(frontId);
        when(documents.upload(eq(application.id()), eq(OnboardingDocumentCategory.DNI_BACK), eq(command.dniBack()), any()))
                .thenReturn(backId);
        when(finalizer.complete(command, frontId, backId)).thenReturn(expected);

        assertThat(service.submit(command)).isEqualTo(expected);

        verify(documents).upload(eq(application.id()), eq(OnboardingDocumentCategory.DNI_FRONT), eq(command.dniFront()),
                org.mockito.ArgumentMatchers.matches("[0-9a-f]{64}"));
        verify(documents).upload(eq(application.id()), eq(OnboardingDocumentCategory.DNI_BACK), eq(command.dniBack()),
                org.mockito.ArgumentMatchers.matches("[0-9a-f]{64}"));
        verify(finalizer).complete(command, frontId, backId);
    }

    @Test
    void shouldReturnAnyPostSubmissionStateWithoutRevalidatingOrUploading() {
        OnboardingApplication expiredCredentialSetup = inProgressApplication()
                .submit(NOW.minusSeconds(10))
                .startAutomatedReview(NOW.minusSeconds(9))
                .approve(NOW.minusSeconds(8))
                .startProvisioning(NOW.minusSeconds(7))
                .markCredentialSetupPending(NOW.minusSeconds(6))
                .expireCredentialSetup(NOW.minusSeconds(5));
        when(applications.findByContinuationTokenHash("continuation-hash"))
                .thenReturn(Optional.of(expiredCredentialSetup));

        var result = service.submit(command(false));

        assertThat(result.status()).isEqualTo(OnboardingApplicationStatus.CREDENTIAL_SETUP_EXPIRED);
        verifyNoInteractions(documents, finalizer);
    }

    @Test
    void shouldRejectMissingTermsBeforeAnyDocumentUpload() {
        when(applications.findByContinuationTokenHash("continuation-hash"))
                .thenReturn(Optional.of(inProgressApplication()));

        assertThatThrownBy(() -> service.submit(command(false)))
                .isInstanceOf(TermsAcceptanceRequiredException.class);

        verifyNoInteractions(documents, finalizer);
    }

    @Test
    void shouldRejectUnknownContinuationBeforeAnySideEffect() {
        when(applications.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(command(true)))
                .isInstanceOf(InvalidContinuationTokenException.class);

        verifyNoInteractions(documents, finalizer);
    }

    private OnboardingApplication inProgressApplication() {
        return OnboardingApplication.start(
                "person@example.com",
                "magic-hash",
                NOW.plusSeconds(1800),
                NOW.plus(Duration.ofDays(15)),
                NOW.minusSeconds(60)
        ).verifyEmail("continuation-hash", NOW.plusSeconds(7200), NOW.minusSeconds(30));
    }

    private SubmitOnboardingCommand command(boolean termsAccepted) {
        return new SubmitOnboardingCommand(
                CONTINUATION_TOKEN,
                "Federico",
                null,
                "Bacelar",
                LocalDate.of(1990, 1, 1),
                "AR",
                ApplicantDocumentType.DNI,
                "30111222",
                "AR",
                LocalDate.of(2030, 1, 1),
                "+5491111111111",
                "Calle",
                "1",
                "Ciudad",
                "Buenos Aires",
                "1000",
                "AR",
                termsAccepted,
                upload("dni-front.png", "front"),
                upload("dni-back.png", "back")
        );
    }

    private OnboardingDocumentUpload upload(String filename, String content) {
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new OnboardingDocumentUpload(
                filename,
                "image/png",
                bytes.length,
                () -> new ByteArrayInputStream(bytes)
        );
    }
}
