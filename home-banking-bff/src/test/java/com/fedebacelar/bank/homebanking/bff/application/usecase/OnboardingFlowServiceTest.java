package com.fedebacelar.bank.homebanking.bff.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.exception.OnboardingSessionRequiredException;
import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingApplicantData;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingContinuation;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingFile;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingNextAction;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSession;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubmission;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingFlowServiceTest {

    private static final String ACCESS_TOKEN = "internal-token";
    private static final String CONTINUATION_TOKEN = "continuation-token";

    private final OnboardingServicePort onboardingServicePort = mock(OnboardingServicePort.class);
    private final GetInternalAccessTokenPort internalAccessTokenPort = mock(GetInternalAccessTokenPort.class);
    private final OnboardingFlowService service = new OnboardingFlowService(
            onboardingServicePort,
            internalAccessTokenPort
    );

    @Test
    void shouldStartApplicationUsingOnboardingMachineCredentials() {
        when(internalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);

        service.startApplication("applicant@example.com");

        verify(onboardingServicePort).startApplication("applicant@example.com", ACCESS_TOKEN);
    }

    @Test
    void shouldConsumeMagicLinkUsingOnboardingMachineCredentials() {
        OnboardingContinuation continuation = new OnboardingContinuation(
                UUID.randomUUID(),
                OnboardingState.IN_PROGRESS,
                CONTINUATION_TOKEN,
                Instant.parse("2026-07-10T14:00:00Z")
        );
        when(internalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboardingServicePort.consumeMagicLink("magic-token", ACCESS_TOKEN)).thenReturn(continuation);

        assertThat(service.consumeMagicLink("magic-token")).isEqualTo(continuation);
    }

    @Test
    void shouldSubmitOneCompositeApplicationCommand() {
        OnboardingApplicantData applicantData = applicantData();
        OnboardingFile dniFront = mock(OnboardingFile.class);
        OnboardingFile dniBack = mock(OnboardingFile.class);
        OnboardingSubmission submission = new OnboardingSubmission(
                UUID.randomUUID(),
                OnboardingState.SUBMITTED,
                Instant.parse("2026-07-10T12:00:00Z"),
                Instant.parse("2026-07-10T12:00:00Z")
        );
        when(internalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboardingServicePort.submit(
                CONTINUATION_TOKEN,
                applicantData,
                true,
                dniFront,
                dniBack,
                ACCESS_TOKEN
        )).thenReturn(submission);

        assertThat(service.submit(CONTINUATION_TOKEN, applicantData, true, dniFront, dniBack))
                .isEqualTo(submission);
    }

    @Test
    void shouldRejectCompositeSubmissionWithoutContinuationCookie() {
        assertThatThrownBy(() -> service.submit(null, applicantData(), true, mock(OnboardingFile.class), mock(OnboardingFile.class)))
                .isInstanceOf(OnboardingSessionRequiredException.class);

        verifyNoInteractions(internalAccessTokenPort, onboardingServicePort);
    }

    @Test
    void shouldReturnOnlyServerDerivedPublicStatus() {
        UUID applicationId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2026-07-10T12:00:00Z");
        OnboardingSession session = OnboardingSession.active(
                applicationId,
                OnboardingState.PROVISIONING,
                Instant.parse("2026-07-11T12:00:00Z"),
                updatedAt
        );
        when(internalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboardingServicePort.validateContinuation(CONTINUATION_TOKEN, ACCESS_TOKEN)).thenReturn(session);

        var status = service.getStatus(CONTINUATION_TOKEN);

        assertThat(status.applicationId()).isEqualTo(applicationId);
        assertThat(status.status()).isEqualTo(OnboardingState.PROVISIONING);
        assertThat(status.nextAction()).isEqualTo(OnboardingNextAction.WAIT);
        assertThat(status.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldResendCredentialInvitationThroughOnboardingService() {
        OnboardingSubmission submission = new OnboardingSubmission(
                UUID.randomUUID(),
                OnboardingState.CREDENTIAL_SETUP_PENDING,
                Instant.parse("2026-07-10T12:00:00Z"),
                Instant.parse("2026-07-10T12:00:00Z")
        );
        when(internalAccessTokenPort.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboardingServicePort.resendCredentialInvitation(CONTINUATION_TOKEN, ACCESS_TOKEN))
                .thenReturn(submission);

        assertThat(service.resendCredentialInvitation(CONTINUATION_TOKEN)).isEqualTo(submission);
        verify(onboardingServicePort).resendCredentialInvitation(CONTINUATION_TOKEN, ACCESS_TOKEN);
    }

    private OnboardingApplicantData applicantData() {
        return new OnboardingApplicantData(
                null,
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
                null,
                null
        );
    }
}
