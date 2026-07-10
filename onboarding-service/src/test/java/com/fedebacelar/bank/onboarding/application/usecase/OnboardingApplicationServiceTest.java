package com.fedebacelar.bank.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.AcceptTermsCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveApplicantDataCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveDocumentReferenceCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.domain.enums.ApplicantDocumentType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingDocumentCategory;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class OnboardingApplicationServiceTest {

    private final OnboardingApplicationRepositoryPort repositoryPort = mock(OnboardingApplicationRepositoryPort.class);
    private final OnboardingApplicantDataRepositoryPort applicantDataRepositoryPort = mock(OnboardingApplicantDataRepositoryPort.class);
    private final OnboardingDocumentReferenceRepositoryPort documentReferenceRepositoryPort = mock(OnboardingDocumentReferenceRepositoryPort.class);
    private final OnboardingTermsAcceptanceRepositoryPort termsAcceptanceRepositoryPort = mock(OnboardingTermsAcceptanceRepositoryPort.class);
    private final TokenGeneratorPort tokenGeneratorPort = mock(TokenGeneratorPort.class);
    private final TokenHashingPort tokenHashingPort = mock(TokenHashingPort.class);
    private final NotificationPort notificationPort = mock(NotificationPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-05T10:00:00Z"), ZoneOffset.UTC);
    private final OnboardingApplicationService service = new OnboardingApplicationService(
            repositoryPort,
            applicantDataRepositoryPort,
            documentReferenceRepositoryPort,
            termsAcceptanceRepositoryPort,
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
        when(repositoryPort.findFirstByEmailAndStatusInOrderByCreatedAtDesc(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any()))
                .thenReturn(Optional.empty());
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
    void resendsMagicLinkForPendingApplication() {
        OnboardingApplication application = pendingApplication("old-magic-hash", Instant.parse("2026-07-05T09:30:00Z"));
        when(repositoryPort.findFirstByEmailAndStatusInOrderByCreatedAtDesc(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any()))
                .thenReturn(Optional.of(application));
        when(tokenGeneratorPort.generate()).thenReturn("new-magic-token");
        when(tokenHashingPort.hash("new-magic-token")).thenReturn("new-magic-hash");
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.start(new StartOnboardingApplicationCommand("person@example.com"));

        assertThat(details.id()).isEqualTo(application.id());
        assertThat(details.email()).isEqualTo("person@example.com");
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING);
        assertThat(details.magicLinkExpiresAt()).isEqualTo(Instant.parse("2026-07-05T10:30:00Z"));
        assertThat(details.updatedAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
        verify(notificationPort).sendMagicLink(
                eq(application.id()),
                eq("person@example.com"),
                eq("http://localhost:4200/onboarding/continue?token=new-magic-token"),
                eq(Duration.ofMinutes(30))
        );
    }

    @Test
    void resendsAccessLinkForInProgressApplication() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        when(repositoryPort.findFirstByEmailAndStatusInOrderByCreatedAtDesc(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any()))
                .thenReturn(Optional.of(application));
        when(tokenGeneratorPort.generate()).thenReturn("new-magic-token");
        when(tokenHashingPort.hash("new-magic-token")).thenReturn("new-magic-hash");
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.start(new StartOnboardingApplicationCommand("person@example.com"));

        assertThat(details.id()).isEqualTo(application.id());
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.IN_PROGRESS);
        assertThat(details.magicLinkExpiresAt()).isEqualTo(Instant.parse("2026-07-05T10:30:00Z"));
        verify(notificationPort).sendMagicLink(
                eq(application.id()),
                eq("person@example.com"),
                eq("http://localhost:4200/onboarding/continue?token=new-magic-token"),
                eq(Duration.ofMinutes(30))
        );
    }

    @Test
    void rejectsDuplicateApplicationAfterSubmission() {
        OnboardingApplication application = applicationWithStatus(OnboardingApplicationStatus.SUBMITTED);
        when(repositoryPort.findFirstByEmailAndStatusInOrderByCreatedAtDesc(eq("person@example.com"), ArgumentMatchers.<Set<OnboardingApplicationStatus>>any()))
                .thenReturn(Optional.of(application));

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
    void consumesFreshAccessLinkForInProgressApplication() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"))
                .refreshAccessLink("new-magic-hash", Instant.parse("2026-07-05T10:30:00Z"), Instant.parse("2026-07-05T10:05:00Z"));
        when(tokenHashingPort.hash("new-magic-token")).thenReturn("new-magic-hash");
        when(tokenHashingPort.hash("new-continuation-token")).thenReturn("new-continuation-hash");
        when(repositoryPort.findByMagicLinkTokenHash("new-magic-hash")).thenReturn(Optional.of(application));
        when(tokenGeneratorPort.generate()).thenReturn("new-continuation-token");
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.consume(new ConsumeMagicLinkCommand("new-magic-token"));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.status()).isEqualTo(OnboardingApplicationStatus.IN_PROGRESS);
        assertThat(details.continuationToken()).isEqualTo("new-continuation-token");
        assertThat(details.continuationExpiresAt()).isEqualTo(Instant.parse("2026-07-05T12:00:00Z"));
    }

    @Test
    void rejectsConsumedMagicLinkForInProgressApplication() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        when(tokenHashingPort.hash("magic-token")).thenReturn("magic-hash");
        when(repositoryPort.findByMagicLinkTokenHash("magic-hash")).thenReturn(Optional.of(application));
        when(tokenGeneratorPort.generate()).thenReturn("new-continuation-token");

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
    void savesApplicantDataWhenContinuationTokenIsValid() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(applicantDataRepositoryPort.findByApplicationId(application.id())).thenReturn(Optional.empty());
        when(applicantDataRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.save(applicantDataCommand("continuation-token"));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.firstName()).isEqualTo("Federico");
        assertThat(details.middleName()).isNull();
        assertThat(details.lastName()).isEqualTo("Bacelar");
        assertThat(details.nationality()).isEqualTo("AR");
        assertThat(details.documentType()).isEqualTo(ApplicantDocumentType.DNI);
        assertThat(details.documentIssuingCountry()).isEqualTo("AR");
        assertThat(details.country()).isEqualTo("AR");
    }

    @Test
    void updatesApplicantDataWhenApplicationAlreadyHasData() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        ApplicantData existingData = ApplicantData.create(
                application.id(),
                "Old",
                null,
                "Name",
                LocalDate.parse("1990-01-01"),
                "AR",
                ApplicantDocumentType.DNI,
                "100",
                "AR",
                null,
                "+5491100000000",
                "Old Street",
                "1",
                "Old City",
                "Old Province",
                "1000",
                "AR",
                Instant.parse("2026-07-05T09:00:00Z")
        );
        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(applicantDataRepositoryPort.findByApplicationId(application.id())).thenReturn(Optional.of(existingData));
        when(applicantDataRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.save(applicantDataCommand("continuation-token"));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.firstName()).isEqualTo("Federico");
        assertThat(details.createdAt()).isEqualTo(Instant.parse("2026-07-05T09:00:00Z"));
        assertThat(details.updatedAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
    }

    @Test
    void savesDocumentReferenceWhenContinuationTokenIsValid() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        UUID documentId = UUID.randomUUID();

        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(documentReferenceRepositoryPort.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_FRONT))
                .thenReturn(Optional.empty());
        when(documentReferenceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.save(new SaveDocumentReferenceCommand(
                "continuation-token",
                OnboardingDocumentCategory.DNI_FRONT,
                documentId
        ));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.category()).isEqualTo(OnboardingDocumentCategory.DNI_FRONT);
        assertThat(details.documentId()).isEqualTo(documentId);
        assertThat(details.createdAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
    }

    @Test
    void updatesDocumentReferenceWhenCategoryAlreadyExists() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        UUID oldDocumentId = UUID.randomUUID();
        UUID newDocumentId = UUID.randomUUID();
        OnboardingDocumentReference existingReference = OnboardingDocumentReference.create(
                application.id(),
                OnboardingDocumentCategory.DNI_BACK,
                oldDocumentId,
                Instant.parse("2026-07-05T09:00:00Z")
        );

        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(documentReferenceRepositoryPort.findByApplicationIdAndCategory(application.id(), OnboardingDocumentCategory.DNI_BACK))
                .thenReturn(Optional.of(existingReference));
        when(documentReferenceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.save(new SaveDocumentReferenceCommand(
                "continuation-token",
                OnboardingDocumentCategory.DNI_BACK,
                newDocumentId
        ));

        assertThat(details.id()).isEqualTo(existingReference.id());
        assertThat(details.documentId()).isEqualTo(newDocumentId);
        assertThat(details.createdAt()).isEqualTo(Instant.parse("2026-07-05T09:00:00Z"));
        assertThat(details.updatedAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
    }

    @Test
    void acceptsTermsWhenContinuationTokenIsValid() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));

        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(termsAcceptanceRepositoryPort.findByApplicationId(application.id())).thenReturn(Optional.empty());
        when(termsAcceptanceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.accept(new AcceptTermsCommand(
                "continuation-token",
                true,
                " ONBOARDING_TERMS_AR_V1 "
        ));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.termsVersion()).isEqualTo("ONBOARDING_TERMS_AR_V1");
        assertThat(details.acceptedAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
    }

    @Test
    void updatesTermsAcceptanceWhenAlreadyAccepted() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));
        OnboardingTermsAcceptance existingAcceptance = OnboardingTermsAcceptance.accept(
                application.id(),
                "ONBOARDING_TERMS_AR_V1",
                Instant.parse("2026-07-05T09:00:00Z")
        );

        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));
        when(termsAcceptanceRepositoryPort.findByApplicationId(application.id())).thenReturn(Optional.of(existingAcceptance));
        when(termsAcceptanceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var details = service.accept(new AcceptTermsCommand(
                "continuation-token",
                true,
                "ONBOARDING_TERMS_AR_V2"
        ));

        assertThat(details.applicationId()).isEqualTo(application.id());
        assertThat(details.termsVersion()).isEqualTo("ONBOARDING_TERMS_AR_V2");
        assertThat(details.createdAt()).isEqualTo(Instant.parse("2026-07-05T09:00:00Z"));
        assertThat(details.updatedAt()).isEqualTo(Instant.parse("2026-07-05T10:00:00Z"));
    }

    @Test
    void rejectsTermsWhenAcceptanceIsFalse() {
        OnboardingApplication application = pendingApplication("magic-hash", Instant.parse("2026-07-05T10:30:00Z"))
                .verifyEmail("continuation-hash", Instant.parse("2026-07-05T12:00:00Z"), Instant.parse("2026-07-05T10:00:00Z"));

        when(tokenHashingPort.hash("continuation-token")).thenReturn("continuation-hash");
        when(repositoryPort.findByContinuationTokenHash("continuation-hash")).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.accept(new AcceptTermsCommand(
                "continuation-token",
                false,
                "ONBOARDING_TERMS_AR_V1"
        ))).isInstanceOf(TermsAcceptanceRequiredException.class);
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

    private OnboardingApplication applicationWithStatus(OnboardingApplicationStatus status) {
        Instant now = Instant.parse("2026-07-05T10:00:00Z");
        return new OnboardingApplication(
                UUID.randomUUID(),
                "person@example.com",
                status,
                "magic-hash",
                now.plus(Duration.ofMinutes(30)),
                now,
                now,
                "continuation-hash",
                now.plus(Duration.ofHours(2)),
                now.plus(Duration.ofDays(15)),
                now,
                now,
                0L
        );
    }

    private SaveApplicantDataCommand applicantDataCommand(String continuationToken) {
        return new SaveApplicantDataCommand(
                continuationToken,
                " Federico ",
                " ",
                " Bacelar ",
                LocalDate.parse("1990-05-10"),
                "ar",
                ApplicantDocumentType.DNI,
                " 12345678 ",
                "ar",
                null,
                "+5491122223333",
                "Av Siempre Viva",
                "742",
                "Buenos Aires",
                "Buenos Aires",
                "1000",
                "ar"
        );
    }
}
