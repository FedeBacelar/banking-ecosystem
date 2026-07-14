package com.fedebacelar.bank.homebanking.bff.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.homebanking.bff.application.port.out.GetInternalAccessTokenPort;
import com.fedebacelar.bank.homebanking.bff.application.port.out.InternalAccessPurpose;
import com.fedebacelar.bank.homebanking.bff.application.port.out.OnboardingServicePort;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingCompletionState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingState;
import com.fedebacelar.bank.homebanking.bff.domain.model.OnboardingSubjectStatus;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GetOnboardingCompletionStatusServiceTest {

    private static final String SUBJECT = "keycloak-user-id";
    private static final String ACCESS_TOKEN = "onboarding-machine-token";
    private static final Instant UPDATED_AT = Instant.parse("2026-07-13T12:00:00Z");

    private final OnboardingServicePort onboarding = mock(OnboardingServicePort.class);
    private final GetInternalAccessTokenPort tokens = mock(GetInternalAccessTokenPort.class);
    private final GetOnboardingCompletionStatusService service =
            new GetOnboardingCompletionStatusService(onboarding, tokens);

    @ParameterizedTest
    @MethodSource("statusMappings")
    void mapsInternalStatesToTheClosedPublicCompletionContract(
            OnboardingState internal,
            OnboardingCompletionState expected
    ) {
        when(tokens.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboarding.getCompletionStatus(SUBJECT, ACCESS_TOKEN)).thenReturn(
                new OnboardingSubjectStatus(internal, UPDATED_AT)
        );

        var result = service.getForKeycloakSubject(SUBJECT);

        assertThat(result.status()).isEqualTo(expected);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void resolvesTheSubjectWithOnboardingMachineCredentials() {
        when(tokens.getAccessToken(InternalAccessPurpose.ONBOARDING)).thenReturn(ACCESS_TOKEN);
        when(onboarding.getCompletionStatus(SUBJECT, ACCESS_TOKEN)).thenReturn(
                new OnboardingSubjectStatus(OnboardingState.COMPLETED, UPDATED_AT)
        );

        service.getForKeycloakSubject(SUBJECT);

        verify(tokens).getAccessToken(InternalAccessPurpose.ONBOARDING);
        verify(onboarding).getCompletionStatus(SUBJECT, ACCESS_TOKEN);
    }

    private static Stream<Arguments> statusMappings() {
        return Stream.of(
                Arguments.of(OnboardingState.APPROVED, OnboardingCompletionState.PROCESSING),
                Arguments.of(OnboardingState.PROVISIONING, OnboardingCompletionState.PROCESSING),
                Arguments.of(OnboardingState.CREDENTIAL_SETUP_PENDING, OnboardingCompletionState.PROCESSING),
                Arguments.of(OnboardingState.COMPLETED, OnboardingCompletionState.COMPLETED),
                Arguments.of(OnboardingState.EMAIL_VERIFICATION_PENDING, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.IN_PROGRESS, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.SUBMITTED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.UNDER_AUTOMATED_REVIEW, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.REVIEW_FAILED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.PROVISIONING_FAILED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.CREDENTIAL_SETUP_EXPIRED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.CREDENTIAL_SETUP_FAILED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.REJECTED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.EXPIRED, OnboardingCompletionState.FAILED),
                Arguments.of(OnboardingState.CANCELLED, OnboardingCompletionState.FAILED)
        );
    }
}
