package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.mapper.OnboardingApplicationDetailsMapper;
import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.MagicLinkFactoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingEmailRequestGuardPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingTelemetryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingReviewPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.PayloadCipherPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidMagicLinkTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.MagicLinkDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService implements
        StartOnboardingApplicationUseCase,
        ConsumeMagicLinkUseCase,
        ValidateContinuationUseCase,
        GetOnboardingApplicationUseCase {

    private static final Set<OnboardingApplicationStatus> ACCESS_RECOVERABLE_STATUSES =
            java.util.Arrays.stream(OnboardingApplicationStatus.values())
                    .filter(OnboardingApplicationStatus::allowsApplicantAccessRecovery)
                    .collect(Collectors.toUnmodifiableSet());

    private final OnboardingApplicationRepositoryPort repository;
    private final OnboardingStatusHistoryRepositoryPort statusHistoryRepository;
    private final OnboardingEmailRequestGuardPort emailRequestGuard;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHashingPort tokenHashing;
    private final MagicLinkDeliveryRepositoryPort deliveryRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final PayloadCipherPort payloadCipher;
    private final MagicLinkFactoryPort magicLinkFactory;
    private final Clock clock;
    private final Duration magicLinkTtl;
    private final Duration continuationTtl;
    private final Duration applicationTtl;
    private final Duration magicLinkRequestCooldown;
    private final OnboardingReviewPolicyPort reviewPolicy;
    private final OnboardingTelemetryPort telemetry;

    public OnboardingApplicationService(
            OnboardingApplicationRepositoryPort repository,
            OnboardingStatusHistoryRepositoryPort statusHistoryRepository,
            OnboardingEmailRequestGuardPort emailRequestGuard,
            TokenGeneratorPort tokenGenerator,
            TokenHashingPort tokenHashing,
            MagicLinkDeliveryRepositoryPort deliveryRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            PayloadCipherPort payloadCipher,
            MagicLinkFactoryPort magicLinkFactory,
            Clock clock,
            OnboardingReviewPolicyPort reviewPolicy,
            OnboardingTelemetryPort telemetry,
            @Value("${onboarding.magic-link.ttl-minutes:30}") long magicLinkTtlMinutes,
            @Value("${onboarding.continuation.ttl-minutes:120}") long continuationTtlMinutes,
            @Value("${onboarding.application.ttl-days:15}") long applicationTtlDays,
            @Value("${onboarding.magic-link.request-cooldown:PT1M}") Duration magicLinkRequestCooldown
    ) {
        this.repository = repository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.emailRequestGuard = emailRequestGuard;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashing = tokenHashing;
        this.deliveryRepository = deliveryRepository;
        this.workItemRepository = workItemRepository;
        this.payloadCipher = payloadCipher;
        this.magicLinkFactory = magicLinkFactory;
        this.clock = clock;
        this.reviewPolicy = reviewPolicy;
        this.telemetry = telemetry;
        this.magicLinkTtl = Duration.ofMinutes(magicLinkTtlMinutes);
        this.continuationTtl = Duration.ofMinutes(continuationTtlMinutes);
        this.applicationTtl = Duration.ofDays(applicationTtlDays);
        this.magicLinkRequestCooldown = magicLinkRequestCooldown;
    }

    @Override
    @Transactional
    public OnboardingApplicationDetails start(StartOnboardingApplicationCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now(clock);
        boolean requestAllowed = emailRequestGuard.acquireAndRegister(
                email, now, magicLinkRequestCooldown
        );
        return repository.findFirstByEmailAndStatusInOrderByCreatedAtDesc(email, ACCESS_RECOVERABLE_STATUSES)
                .filter(application -> requestAllowed)
                .map(application -> refreshOrReplaceExpired(application, now))
                .orElseGet(() -> existingOrCreate(email, now));
    }

    private OnboardingApplicationDetails existingOrCreate(String email, Instant now) {
        return repository.findFirstByEmailAndStatusInOrderByCreatedAtDesc(email, ACCESS_RECOVERABLE_STATUSES)
                .map(OnboardingApplicationDetailsMapper::toDetails)
                .orElseGet(() -> createApplication(email, now));
    }

    private OnboardingApplicationDetails refreshOrReplaceExpired(OnboardingApplication application, Instant now) {
        if (!now.isBefore(application.expiresAt())) {
            if (application.canExpire()) {
                OnboardingApplication expired = repository.save(application.expire(now));
                saveHistory(expired, application.status(), expired.status(), "APPLICATION_EXPIRED", now);
                return createApplication(application.email(), now);
            }
            return OnboardingApplicationDetailsMapper.toDetails(application);
        }

        String token = tokenGenerator.generate();
        OnboardingApplication refreshed = refreshAccessLink(application, token, now);
        OnboardingApplication saved = repository.save(refreshed);
        enqueueMagicLink(saved, token, now);
        return OnboardingApplicationDetailsMapper.toDetails(saved);
    }

    private OnboardingApplication refreshAccessLink(OnboardingApplication application, String token, Instant now) {
        String tokenHash = tokenHashing.hash(token);
        Instant expiresAt = now.plus(magicLinkTtl);
        if (application.status() == OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
            return application.refreshMagicLink(tokenHash, expiresAt, now);
        }
        return application.refreshAccessLink(tokenHash, expiresAt, now);
    }

    private OnboardingApplicationDetails createApplication(String email, Instant now) {
        String token = tokenGenerator.generate();
        OnboardingApplication application = OnboardingApplication.start(
                email,
                tokenHashing.hash(token),
                now.plus(magicLinkTtl),
                now.plus(applicationTtl),
                reviewPolicy.mode(),
                reviewPolicy.activePolicyVersion(),
                now
        );
        OnboardingApplication saved = repository.save(application);
        saveHistory(saved, null, saved.status(), "APPLICATION_STARTED", now);
        enqueueMagicLink(saved, token, now);
        telemetry.recordApplicationEvent(OnboardingTelemetryPort.ApplicationEvent.CREATED);
        return OnboardingApplicationDetailsMapper.toDetails(saved);
    }

    private void enqueueMagicLink(OnboardingApplication application, String token, Instant now) {
        String encryptedMagicLink = payloadCipher.encrypt(magicLinkFactory.create(token));
        MagicLinkDelivery delivery = deliveryRepository.findByApplicationId(application.id())
                .map(existing -> existing.replace(
                        application.email(), encryptedMagicLink, application.magicLinkExpiresAt(), now
                ))
                .orElseGet(() -> MagicLinkDelivery.pending(
                        application.id(), application.email(), encryptedMagicLink,
                        application.magicLinkExpiresAt(), now
                ));
        deliveryRepository.save(delivery);
        workItemRepository.findByApplicationIdAndJobType(application.id(), WorkflowJobType.MAGIC_LINK_DELIVERY)
                .map(existing -> existing.reset(now))
                .map(workItemRepository::save)
                .orElseGet(() -> workItemRepository.save(OnboardingWorkItem.pending(
                        application.id(), WorkflowJobType.MAGIC_LINK_DELIVERY, now
                )));
    }

    @Override
    @Transactional(noRollbackFor = OnboardingMagicLinkExpiredException.class)
    public OnboardingContinuationDetails consume(ConsumeMagicLinkCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = repository.findByMagicLinkTokenHashForUpdate(tokenHashing.hash(command.token()))
                .orElseThrow(InvalidMagicLinkTokenException::new);

        if (application.magicLinkExpired(now) || !now.isBefore(application.expiresAt())) {
            expireIfAllowed(application, "MAGIC_LINK_EXPIRED", now);
            throw new OnboardingMagicLinkExpiredException();
        }

        String continuationToken = tokenGenerator.generate();
        OnboardingApplication continued = continueApplication(application, continuationToken, now);
        OnboardingApplication saved = repository.save(continued);
        if (application.status() != saved.status()) {
            saveHistory(saved, application.status(), saved.status(), "EMAIL_VERIFIED", now);
        }
        return new OnboardingContinuationDetails(
                saved.id(), saved.email(), saved.status(), continuationToken, saved.continuationExpiresAt()
        );
    }

    private OnboardingApplication continueApplication(OnboardingApplication application, String token, Instant now) {
        if (application.magicLinkConsumed()) {
            throw new OnboardingMagicLinkAlreadyConsumedException();
        }
        String continuationHash = tokenHashing.hash(token);
        Instant continuationExpiresAt = now.plus(continuationTtl);
        if (application.status() == OnboardingApplicationStatus.EMAIL_VERIFICATION_PENDING) {
            return application.verifyEmail(continuationHash, continuationExpiresAt, now);
        }
        if (application.allowsApplicantAccessRecovery()) {
            return application.renewContinuation(continuationHash, continuationExpiresAt, now);
        }
        throw new OnboardingMagicLinkAlreadyConsumedException();
    }

    @Override
    @Transactional(noRollbackFor = OnboardingContinuationExpiredException.class)
    public OnboardingApplicationDetails validate(ValidateContinuationCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = repository.findByContinuationTokenHash(tokenHashing.hash(command.token()))
                .orElseThrow(InvalidContinuationTokenException::new);
        if (application.continuationExpired(now) || !now.isBefore(application.expiresAt())) {
            if (!now.isBefore(application.expiresAt())) {
                expireIfAllowed(application, "APPLICATION_EXPIRED", now);
            }
            throw new OnboardingContinuationExpiredException();
        }
        return OnboardingApplicationDetailsMapper.toDetails(application);
    }

    private void expireIfAllowed(OnboardingApplication application, String reason, Instant now) {
        if (!application.canExpire()) {
            return;
        }
        OnboardingApplication expired = repository.save(application.expire(now));
        saveHistory(expired, application.status(), expired.status(), reason, now);
    }

    private void saveHistory(
            OnboardingApplication application,
            OnboardingApplicationStatus previousStatus,
            OnboardingApplicationStatus newStatus,
            String reason,
            Instant now
    ) {
        statusHistoryRepository.save(OnboardingStatusHistory.transition(
                application.id(), previousStatus, newStatus, reason, OnboardingActorType.APPLICANT_SESSION, now
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationDetails get(UUID applicationId) {
        return repository.findById(applicationId)
                .map(OnboardingApplicationDetailsMapper::toDetails)
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(applicationId));
    }
}
