package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class OnboardingApplicationServiceTest {

    private final OnboardingApplicationRepositoryPort repositoryPort = mock(OnboardingApplicationRepositoryPort.class);
    private final TokenGeneratorPort tokenGeneratorPort = mock(TokenGeneratorPort.class);
    private final TokenHashingPort tokenHashingPort = mock(TokenHashingPort.class);
    private final NotificationPort notificationPort = mock(NotificationPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-05T10:00:00Z"), ZoneOffset.UTC);
    private final OnboardingApplicationService service = new OnboardingApplicationService(
            repositoryPort,
            tokenGeneratorPort,
            tokenHashingPort,
            notificationPort,
            clock,
            30,
            120,
            15,
            "http://localhost:4200/onboarding/continue"
    );

    @Test
    void startsApplicationAndSendsMagicLink() {
        when(repositoryPort.existsByEmailAndStatusIn(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any())).thenReturn(false);
        when(tokenGeneratorPort.generate()).thenReturn("magic-token");
        when(tokenHashingPort.hash("magic-token")).thenReturn("magic-hash");
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.start(new StartOnboardingApplicationCommand("Person@Example.com "));

        assertThat(details.email()).isEqualTo("person@example.com");
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING);
        assertThat(details.magicLinkExpiresAt()).isEqualTo(Instant.parse("2026-07-05T10:30:00Z"));
        assertThat(details.expiresAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
        verify(notificationPort).sendMagicLink(
                eq(details.id()),
                eq("person@example.com"),
                eq("http://localhost:4200/onboarding/continue?token=magic-token"),
                eq(Duration.ofMinutes(30))
        );
    }

    @Test
    void rejectsDuplicateActiveApplication() {
        when(repositoryPort.existsByEmailAndStatusIn(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any())).thenReturn(true);

        assertThatThrownBy(() -> service.start(new StartOnboardingApplicationCommand("person@example.com")))
                .isInstanceOf(DuplicateActiveOnboardingApplicationException.class);
    }

    @Test
    void consumesMagicLinkAndReturnsContinuationToken() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"));
        when(tokenHashingPort.hash("magic-token")).thenReturn("magic-hash");
        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByMagicLinkTokenHash("magic-hash")).thenReturn(Optional.of(application));
        when(tokenGeneratorPort.generate()).thenReturn("continuation-token");
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.consume(new ConsumeMagicLinkCommand("magic-token"));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.IN_PROGRESS);
        assertThat(details.continuationToken()).isEqualTo("continuation-token");
        assertThat(details.continuationExpiresAt()).isEqualTo(Instant.parse("2026-07-05T12:00:00Z"));
    }

    @Test
    void rejectsConsumedMagicLink() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        when(tokenHashingPort.hash("magic-token")).thenReturn("magic-hash");
        when(repositoryPort.findByMagicLinkTokenHash("magic-hash")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.consume(new ConsumeMagicLinkCommand("magic-token")))
                .isInstanceOf(OnboardingMagicLinkAlreadyConsumedException.class);
    }

    @Test
    void expiresApplicationWhenMagicLinkExpired() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T09:59:59Z"));
        when(tokenHashingPort.hash("magic-token")).thenReturn("magic-hash");
        when(repositoryPort.findByMagicLinkTokenHash("magic-hash")).thenReturn(Optional.of(application));
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.consume(new ConsumeMagicLinkCommand("magic-token")))
                .isInstanceOf(OnboardingMagicLinkExpiredException.class);
    }

    @Test
    void validatesContinuationToken() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));

        var details = service.validate(new ValidateContinuationCommand("continuation-token"));

        assertThat(details.id()).isEqualTo(application.id());
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.IN_PROGRESS);
    }

    @Test
    void rejectsExpiredContinuationToken() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T09:59:59Z"), Instant.parse("2026-07-05T09:00:00Z"));
        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.validate(new ValidateContinuationCommand("continuation-token")))
                .isInstanceOf(OnboardingContinuationExpiredException.class);
    }

    private OnboardingApplication pendingApplication(String magicLinkHash, Instant magicLinkExpiresAt) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return OnboardingApplication.start(
                "person@example.com",
                magicLinkHash,
                magicLinkExpiresAt,
                now.plus(Duration.ofDays(15)),
                now
        );
    }
}
