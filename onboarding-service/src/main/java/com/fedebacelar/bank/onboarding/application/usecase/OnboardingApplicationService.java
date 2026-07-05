package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.command.ConsumeMagicLinkCommand;
import com.fedebacelar.bank.onboarding.application.command.StartOnboardingApplicationCommand;
import com.fedebacelar.bank.onboarding.application.command.ValidateContinuationCommand;
import com.fedebacelar.bank.onboarding.application.mapper.OnboardingApplicationDetailsMapper;
import com.fedebacelar.bank.onboarding.application.port.in.ConsumeMagicLinkUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.GetOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.StartOnboardingApplicationUseCase;
import com.fedebacelar.bank.onboarding.application.port.in.ValidateContinuationUseCase;
import com.fedebacelar.bank.onboarding.application.port.out.NotificationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenGeneratorPort;
import com.fedebacelar.bank.onboarding.application.port.out.TokenHashingPort;
import com.fedebacelar.bank.onboarding.application.view.OnboardingApplicationDetails;
import com.fedebacelar.bank.onboarding.application.view.OnboardingContinuationDetails;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.exception.DuplicateActiveOnboardingApplicationException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidContinuationTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.InvalidMagicLinkTokenException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingContinuationExpiredException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkAlreadyConsumedException;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingMagicLinkExpiredException;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
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
        if (repositoryPort.existsByEmailAndStatusIn(email, ACTIVE_STATUSES)) {
            throw new DuplicateActiveOnboardingApplicationException(email);
        }

        Instant now = Instant.now(clock);
        String magicLinkToken = tokenGeneratorPort.generate();
        OnboardingApplication application = OnboardingApplication.start(
                email,
                tokenHashingPort.hash(magicLinkToken),
                now.plus(magicLinkTtl),
                now.plus(applicationTtl),
                now
        );

        OnboardingApplication savedApplication = repositoryPort.save(application);
        notificationPort.sendMagicLink(
                savedApplication.id(),
                savedApplication.email(),
                magicLink(magicLinkToken),
                magicLinkTtl
        );
        return OnboardingApplicationDetailsMapper.toDetails(savedApplication);
    }

    @Override
    @Transactional
    public OnboardingContinuationDetails consume(ConsumeMagicLinkCommand command) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = repositoryPort.findByMagicLinkTokenHash(tokenHashingPort.hash(command.token()))
                .orElseThrow(InvalidMagicLinkTokenException::new);

        if (application.magicLinkConsumed()) {
            throw new OnboardingMagicLinkAlreadyConsumedException();
        }
        if (application.magicLinkExpired(now)) {
            repositoryPort.save(application.expire(now));
            throw new OnboardingMagicLinkExpiredException();
        }

        String continuationToken = tokenGeneratorPort.generate();
        OnboardingApplication verifiedApplication = application.verifyEmail(
                tokenHashingPort.hash(continuationToken),
                now.plus(continuationTtl),
                now
        );
        OnboardingApplication savedApplication = repositoryPort.save(verifiedApplication);
        return new OnboardingContinuationDetails(
                savedApplication.id(),
                savedApplication.email(),
                savedApplication.status(),
                continuationToken,
                savedApplication.continuationExpiresAt()
        );
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
    @Transactional(readOnly = true)
    public OnboardingApplicationDetails get(UUID applicationId) {
        return repositoryPort.findById(applicationId)
                .map(OnboardingApplicationDetailsMapper::toDetails)
                .orElseThrow(() -> new OnboardingApplicationNotFoundException(applicationId));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String magicLink(String token) {
        String separator = frontendMagicLinkBaseUrl.contains("?") ? "&" : "?";
        return frontendMagicLinkBaseUrl + separator + "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
