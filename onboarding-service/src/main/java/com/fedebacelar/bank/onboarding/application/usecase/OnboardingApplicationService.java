package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.AcceptTermsCommand;
import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveApplicantDataCommand;
import com.fedebacelar.bank.onboarding.application.command.SaveDocumentReferenceCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.mapper.ApplicantDataDetailsMapper;
import com.fedebacelar.bank.onboarding.application.mapper.DocumentReferenceDetailsMapper;
import com.fedebacelar.bank.onboarding.application.mapper.OnboardingApplicationDetailsMapper;
import com.fedebacelar.bank.onboarding.application.mapper.TermsAcceptanceDetailsMapper;
import com.fedebacelar.bank.onboarding.application.port.in.AcceptTermsUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveApplicantDataUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.SaveDocumentReferenceUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingDocumentReferenceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTermsAcceptanceRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.ApplicantDataDetails;
import com.fedebacelar.bank.onboarding.application.view.DocumentReferenceDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.application.view.TermsAcceptanceDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidMagicLinkTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidOnboardingStatusTransitionException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.TermsAcceptanceRequiredException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingDocumentReference;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingTermsAcceptance;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService implements
        StartOnboardingApplicationUseCase,
        ConsumeMagicLinkUseCase,
        ValidateContinuationUseCase,
        SaveApplicantDataUseCase,
        SaveDocumentReferenceUseCase,
        AcceptTermsUseCase,
        GetOnboardingApplicationUseCase {

    private static final Set<OnboardingApplicationStatus> ACTIVE_STATUSES = Set.of(
            OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING,
            OnboardingApplicationStatus.IN_PROGRESS,
            OnboardingApplicationStatus.SUBMITTED,
            OnboardingApplicationStatus.UNDER_AUTOMATED_REVIEW,
            OnboardingApplicationStatus.APPROVED,
            OnboardingApplicationStatus.PROVISIONING,
            OnboardingApplicationStatus.CREDENTIAL_SETUP_PENDING,
            OnboardingApplicationStatus.PROVISIONING_FAILED
    );

    private final OnboardingApplicationRepositoryPort repositoryPort;
    private final OnboardingApplicantDataRepositoryPort applicantDataRepositoryPort;
    private final OnboardingDocumentReferenceRepositoryPort documentReferenceRepositoryPort;
    private final OnboardingTermsAcceptanceRepositoryPort termsAcceptanceRepositoryPort;
    private final TokenGeneratorPort tokenGeneratorPort;
    private final TokenHashingPort tokenHashingPort;
    private final NotificationPort notificationPort;
    private final Clock clock;
    private final Duration magicLinkTtl;
    private final Duration continuationTtl;
    private final Duration applicationTtl;
    private final String frontendMagicLinkBaseUrl;

    public OnboardingApplicationService(
            OnboardingApplicationRepositoryPort repositoryPort,
            OnboardingApplicantDataRepositoryPort applicantDataRepositoryPort,
            OnboardingDocumentReferenceRepositoryPort documentReferenceRepositoryPort,
            OnboardingTermsAcceptanceRepositoryPort termsAcceptanceRepositoryPort,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHashingPort tokenHashingPort,
            NotificationPort notificationPort,
            Clock clock,
            @Value("${onboarding.magic-link.ttl-minutes:30}") long magicLinkTtlMinutes,
            @Value("${onboarding.continuation.ttl-minutes:120}") long continuationTtlMinutes,
            @Value("${onboarding.application.ttl-days:15}") long applicationTtlDays,
            @Value("${onboarding.frontend.magic-link-base-url:http://localhost:4200/onboarding/continue}") String frontendMagicLinkBaseUrl
    ) {
        this.repositoryPort = repositoryPort;
        this.applicantDataRepositoryPort = applicantDataRepositoryPort;
        this.documentReferenceRepositoryPort = documentReferenceRepositoryPort;
        this.termsAcceptanceRepositoryPort = termsAcceptanceRepositoryPort;
        this.tokenGeneratorPort = tokenGeneratorPort;
        this.tokenHashingPort = tokenHashingPort;
        this.notificationPort = notificationPort;
        this.clock = clock;
        this.magicLinkTtl = Duration.ofMinutes(magicLinkTtlMinutes);
        this.continuationTtl = Duration.ofMinutes(continuationTtlMinutes);
        this.applicationTtl = Duration.ofDays(applicationTtlDays);
        this.frontendMagicLinkBaseUrl = frontendMagicLinkBaseUrl;
    }

    @Override
    @Transactional
    public OnboardingApplicationDetails start(StartOnboardingApplicationCommand command) {
        String email = normalizeEmail(command.email());
        Instant now = Instant.now(clock);
        return repositoryPort.findFirstByEmailAndStatusInOrderByCreatedAtDesc(email, ACTIVE_STATUSES)
                .map(application -> handleExistingActiveApplication(application, now))
                .orElseGet(() -> createApplication(email, now));
    }

    private OnboardingApplicationDetails handleExistingActiveApplication(OnboardingApplication application, Instant now) {
        if (application.status() != OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING
                && application.status() != OnboardingApplicationStatus.IN_PROGRESS) {
            throw new DuplicateActiveOnboardingApplicationException(application.email());
        }

        String magicLinkToken = tokenGeneratorPort.generate();
        OnboardingApplication refreshedApplication = refreshAccessLink(application, magicLinkToken, now);
        OnboardingApplication savedApplication = repositoryPort.save(refreshedApplication);
        sendMagicLink(savedApplication, magicLinkToken);
        return OnboardingApplicationDetailsMapper.toDetails(savedApplication);
    }

    private OnboardingApplication refreshAccessLink(OnboardingApplication application, String magicLinkToken, Instant now) {
        String magicLinkTokenHash = tokenHashingPort.hash(magicLinkToken);
        Instant magicLinkExpiresAt = now.plus(magicLinkTtl);

        if (application.status() == OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
            return application.refreshMagicLink(magicLinkTokenHash, magicLinkExpiresAt, now);
        }

        return application.refreshAccessLink(magicLinkTokenHash, magicLinkExpiresAt, now);
    }

    private OnboardingApplicationDetails createApplication(String email, Instant now) {
        String magicLinkToken = tokenGeneratorPort.generate();
        OnboardingApplication application = OnboardingApplication.start(
                email,
                tokenHashingPort.hash(magicLinkToken),
                now.plus(magicLinkTtl),
                now.plus(applicationTtl),
                now
        );

        OnboardingApplication savedApplication = repositoryPort.save(application);
        sendMagicLink(savedApplication, magicLinkToken);
        return OnboardingApplicationDetailsMapper.toDetails(savedApplication);
    }

    private void sendMagicLink(OnboardingApplication savedApplication, String magicLinkToken) {
        notificationPort.sendMagicLink(
                savedApplication.id(),
                savedApplication.email(),
                magicLink(magicLinkToken),
                magicLinkTtl
        );
    }

    @Override
    @Transactional
    public OnboardingContinuationDetails consume(ConsumeMagicLinkCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = repositoryPort.findByMagicLinkTokenHash(tokenHashingPort.hash(command.token()))
                .orElseThrow(InvalidMagicLinkTokenException::new);

        if (application.magicLinkExpired(now)) {
            if (application.status() == OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
                repositoryPort.save(application.expire(now));
            }
            throw new OnboardingMagicLinkExpiredException();
        }

        String continuationToken = tokenGeneratorPort.generate();
        OnboardingApplication verifiedApplication = continueApplication(application, continuationToken, now);
        OnboardingApplication savedApplication = repositoryPort.save(verifiedApplication);
        return new OnboardingContinuationDetails(
                savedApplication.id(),
                savedApplication.email(),
                savedApplication.status(),
                continuationToken,
                savedApplication.continuationExpiresAt()
        );
    }

    private OnboardingApplication continueApplication(OnboardingApplication application, String continuationToken, Instant now) {
        String continuationTokenHash = tokenHashingPort.hash(continuationToken);
        Instant continuationExpiresAt = now.plus(continuationTtl);

        if (application.status() == OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
            if (application.magicLinkConsumed()) {
                throw new OnboardingMagicLinkAlreadyConsumedException();
            }
            return application.verifyEmail(continuationTokenHash, continuationExpiresAt, now);
        }

        if (application.status() == OnboardingApplicationStatus.IN_PROGRESS) {
            if (application.magicLinkConsumed()) {
                throw new OnboardingMagicLinkAlreadyConsumedException();
            }
            return application.renewContinuation(continuationTokenHash, continuationExpiresAt, now);
        }

        throw new OnboardingMagicLinkAlreadyConsumedException();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationDetails validate(ValidateContinuationCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = repositoryPort.findByContinuationTokenHash(tokenHashingPort.hash(command.token()))
                .orElseThrow(InvalidContinuationTokenException::new);
        if (application.continuationExpired(now)) {
            throw new OnboardingContinuationExpiredException();
        }
        return OnboardingApplicationDetailsMapper.toDetails(application);
    }

    @Override
    @Transactional
    public ApplicantDataDetails save(SaveApplicantDataCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = requireInProgressApplication(command.continuationToken(), now);

        ApplicantData incomingData = toApplicantData(application.id(), command, now);
        ApplicantData savedData = applicantDataRepositoryPort.findByApplicationId(application.id())
                .map(existingData -> existingData.updateFrom(incomingData, now))
                .map(applicantDataRepositoryPort::save)
                .orElseGet(() -> applicantDataRepositoryPort.save(incomingData));

        return ApplicantDataDetailsMapper.toDetails(savedData);
    }

    @Override
    @Transactional
    public DocumentReferenceDetails save(SaveDocumentReferenceCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = requireInProgressApplication(command.continuationToken(), now);

        OnboardingDocumentReference savedReference = documentReferenceRepositoryPort
                .findByApplicationIdAndCategory(application.id(), command.category())
                .map(reference -> reference.updateDocument(command.documentId(), now))
                .map(documentReferenceRepositoryPort::save)
                .orElseGet(() -> documentReferenceRepositoryPort.save(OnboardingDocumentReference.create(
                        application.id(),
                        command.category(),
                        command.documentId(),
                        now
                )));

        return DocumentReferenceDetailsMapper.toDetails(savedReference);
    }

    @Override
    @Transactional
    public TermsAcceptanceDetails accept(AcceptTermsCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = requireInProgressApplication(command.continuationToken(), now);
        if (!command.accepted()) {
            throw new TermsAcceptanceRequiredException();
        }
        String termsVersion = normalizeRequiredText(command.termsVersion());

        OnboardingTermsAcceptance savedAcceptance = termsAcceptanceRepositoryPort.findByApplicationId(application.id())
                .map(acceptance -> acceptance.update(termsVersion, now))
                .map(termsAcceptanceRepositoryPort::save)
                .orElseGet(() -> termsAcceptanceRepositoryPort.save(OnboardingTermsAcceptance.accept(
                        application.id(),
                        termsVersion,
                        now
                )));

        return TermsAcceptanceDetailsMapper.toDetails(savedAcceptance);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationDetails get(UUID applicationId) {
        return repositoryPort.findById(applicationId)
                .map(OnboardingApplicationDetailsMapper::toDetails)
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(applicationId));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private OnboardingApplication requireInProgressApplication(String continuationToken, Instant now) {
        OnboardingApplication application = repositoryPort.findByContinuationTokenHash(tokenHashingPort.hash(continuationToken))
                .orElseThrow(InvalidContinuationTokenException::new);
        if (application.continuationExpired(now)) {
            throw new OnboardingContinuationExpiredException();
        }
        if (application.status() != OnboardingApplicationStatus.IN_PROGRESS) {
            throw new InvalidOnboardingStatusTransitionException(application.status(), OnboardingApplicationStatus.IN_PROGRESS);
        }
        return application;
    }

    private ApplicantData toApplicantData(UUID applicationId, SaveApplicantDataCommand command, Instant now) {
        return ApplicantData.create(
                applicationId,
                normalizeRequiredText(command.firstName()),
                normalizeOptionalText(command.middleName()),
                normalizeRequiredText(command.lastName()),
                command.birthDate(),
                normalizeCountry(command.nationality()),
                command.documentType(),
                normalizeRequiredText(command.documentNumber()),
                normalizeCountry(command.documentIssuingCountry()),
                command.documentExpirationDate(),
                normalizeRequiredText(command.phoneNumber()),
                normalizeRequiredText(command.street()),
                normalizeRequiredText(command.streetNumber()),
                normalizeRequiredText(command.city()),
                normalizeRequiredText(command.province()),
                normalizeRequiredText(command.postalCode()),
                normalizeCountry(command.country()),
                now
        );
    }

    private String normalizeRequiredText(String value) {
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCountry(String value) {
        return normalizeRequiredText(value).toUpperCase(Locale.ROOT);
    }

    private String magicLink(String token) {
        String separator = frontendMagicLinkBaseUrl.contains("?") ? "&" : "?";
        return frontendMagicLinkBaseUrl + separator + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
