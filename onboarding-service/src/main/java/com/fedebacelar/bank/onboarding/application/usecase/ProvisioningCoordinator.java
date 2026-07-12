package com.fedebacelar.bank.onboarding.application.usecase;

import com.fedebacelar.bank.onboarding.application.port.out.AccountProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.CustomerProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.IdentityProvisioningPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicantDataRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningStepRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingStatusHistoryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingProvisioningPolicyPort;
import com.fedebacelar.bank.onboarding.application.port.out.ProvisioningFailureClassifierPort;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingActorType;
import com.fedebacelar.bank.onboarding.domain.enums.OnboardingApplicationStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepStatus;
import com.fedebacelar.bank.onboarding.domain.enums.ProvisioningStepType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.exception.OnboardingApplicationNotFoundException;
import com.fedebacelar.bank.onboarding.domain.model.ApplicantData;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingProvisioningStep;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingStatusHistory;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ProvisioningCoordinator {
    private static final List<ProvisioningStepType> ORDER = List.of(
            ProvisioningStepType.PRECREATE_KEYCLOAK_USER,
            ProvisioningStepType.CREATE_CUSTOMER,
            ProvisioningStepType.APPROVE_CUSTOMER_KYC,
            ProvisioningStepType.OPEN_ACCOUNT,
            ProvisioningStepType.CREATE_IDENTITY_LINK,
            ProvisioningStepType.SEND_CREDENTIAL_SETUP_EMAIL
    );

    private final OnboardingApplicationRepositoryPort applicationRepository;
    private final OnboardingApplicantDataRepositoryPort applicantRepository;
    private final OnboardingProvisioningStepRepositoryPort stepRepository;
    private final OnboardingStatusHistoryRepositoryPort historyRepository;
    private final OnboardingWorkItemRepositoryPort workItemRepository;
    private final CustomerProvisioningPort customerPort;
    private final AccountProvisioningPort accountPort;
    private final CredentialProvisioningPort credentialPort;
    private final IdentityProvisioningPort identityPort;
    private final OnboardingUniquenessReservationPort reservationPort;
    private final OnboardingProvisioningPolicyPort provisioningPolicy;
    private final ProvisioningFailureClassifierPort failureClassifier;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ProvisioningCoordinator(OnboardingApplicationRepositoryPort applicationRepository,
            OnboardingApplicantDataRepositoryPort applicantRepository,
            OnboardingProvisioningStepRepositoryPort stepRepository,
            OnboardingStatusHistoryRepositoryPort historyRepository,
            OnboardingWorkItemRepositoryPort workItemRepository,
            CustomerProvisioningPort customerPort, AccountProvisioningPort accountPort,
            CredentialProvisioningPort credentialPort, IdentityProvisioningPort identityPort,
            OnboardingUniquenessReservationPort reservationPort,
            OnboardingProvisioningPolicyPort provisioningPolicy,
            ProvisioningFailureClassifierPort failureClassifier,
            TransactionTemplate transactionTemplate, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.applicantRepository = applicantRepository;
        this.stepRepository = stepRepository;
        this.historyRepository = historyRepository;
        this.workItemRepository = workItemRepository;
        this.customerPort = customerPort;
        this.accountPort = accountPort;
        this.credentialPort = credentialPort;
        this.identityPort = identityPort;
        this.reservationPort = reservationPort;
        this.provisioningPolicy = provisioningPolicy;
        this.failureClassifier = failureClassifier;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public void execute(OnboardingWorkItem workItem) {
        Instant now = Instant.now(clock);
        OnboardingApplication application = transactionTemplate.execute(status -> begin(workItem.applicationId(), now));
        if (application == null) throw new IllegalStateException("Could not start provisioning.");
        ApplicantData applicant = applicantRepository.findByApplicationId(application.id())
                .orElseThrow(() -> new IllegalStateException("Applicant data is missing during provisioning."));

        for (ProvisioningStepType type : ORDER) {
            executeStep(application, applicant, type);
        }

        transactionTemplate.executeWithoutResult(status -> complete(workItem, Instant.now(clock)));
    }

    public void handleFailure(OnboardingWorkItem workItem, RuntimeException exception) {
        Instant now = Instant.now(clock);
        boolean retryable = failureClassifier.isRetryable(exception);
        transactionTemplate.executeWithoutResult(status -> {
            stepRepository.findByApplicationId(workItem.applicationId()).stream()
                    .filter(step -> step.status() == ProvisioningStepStatus.RUNNING)
                    .findFirst()
                    .ifPresent(step -> stepRepository.save(retryable && workItem.attempts() < provisioningPolicy.maxAttempts()
                            ? step.retry("PROVISIONING_STEP_ERROR", now.plus(provisioningPolicy.retryDelay(workItem.attempts())), now)
                            : step.fail("PROVISIONING_STEP_ERROR", now)));

            if (retryable && workItem.attempts() < provisioningPolicy.maxAttempts()) {
                workItemRepository.save(workItem.retry("PROVISIONING_EXECUTION_ERROR", now.plus(provisioningPolicy.retryDelay(workItem.attempts())), now));
                return;
            }
            OnboardingApplication application = requireApplication(workItem.applicationId());
            if (application.status() == OnboardingApplicationStatus.APPROVED
                    || application.status() == OnboardingApplicationStatus.PROVISIONING) {
                OnboardingApplication failed = applicationRepository.save(application.markProvisioningFailed(now));
                saveHistory(application, failed, retryable ? "PROVISIONING_RETRIES_EXHAUSTED" : "PROVISIONING_NON_RETRYABLE_FAILURE", now);
            }
            workItemRepository.save(workItem.fail("PROVISIONING_EXECUTION_ERROR", now));
        });
    }

    private OnboardingApplication begin(UUID applicationId, Instant now) {
        OnboardingApplication application = requireApplication(applicationId);
        if (application.status() == OnboardingApplicationStatus.APPROVED) {
            OnboardingApplication provisioning = applicationRepository.save(application.startProvisioning(now));
            saveHistory(application, provisioning, "PROVISIONING_STARTED", now);
            application = provisioning;
        }
        if (application.status() != OnboardingApplicationStatus.PROVISIONING) {
            throw new IllegalStateException("Application is not ready for provisioning.");
        }
        for (ProvisioningStepType type : ORDER) {
            stepRepository.findByApplicationIdAndStepType(applicationId, type)
                    .orElseGet(() -> stepRepository.save(OnboardingProvisioningStep.pending(applicationId, type, now)));
        }
        return application;
    }

    private void executeStep(OnboardingApplication application, ApplicantData applicant, ProvisioningStepType type) {
        OnboardingProvisioningStep step = stepRepository.findByApplicationIdAndStepType(application.id(), type).orElseThrow();
        if (step.status() == ProvisioningStepStatus.SUCCEEDED) return;
        String requestHash = fingerprintFor(application, applicant, type);
        step = stepRepository.save(step.start(requestHash, Instant.now(clock)));
        String reference = switch (type) {
            case CREATE_CUSTOMER -> customerPort.createCustomer(application.id(), application.email(), applicant).toString();
            case APPROVE_CUSTOMER_KYC -> {
                customerPort.approveKyc(reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER));
                yield reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER).toString();
            }
            case OPEN_ACCOUNT -> accountPort.openDefaultAccount(application.id(), reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER)).toString();
            case PRECREATE_KEYCLOAK_USER -> credentialPort.precreateUser(application.id(), application.email(), applicant);
            case CREATE_IDENTITY_LINK -> identityPort.createOrResolve(
                    reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER),
                    stringReference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER)).toString();
            case ACTIVATE_ACCOUNT -> {
                accountPort.activate(reference(application.id(), ProvisioningStepType.OPEN_ACCOUNT));
                yield reference(application.id(), ProvisioningStepType.OPEN_ACCOUNT).toString();
            }
            case SEND_CREDENTIAL_SETUP_EMAIL -> {
                String userId = stringReference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER);
                credentialPort.sendCredentialSetupEmail(userId);
                yield userId;
            }
        };
        stepRepository.save(step.succeed(reference, Instant.now(clock)));
    }

    private void complete(OnboardingWorkItem workItem, Instant now) {
        OnboardingApplication application = requireApplication(workItem.applicationId());
        OnboardingApplication pending = applicationRepository.save(application.markCredentialSetupPending(now));
        saveHistory(application, pending, "CREDENTIAL_SETUP_INVITATION_SENT", now);
        workItemRepository.save(workItem.succeed(now));
        reservationPort.convertByApplicationId(application.id(), now);
        workItemRepository.findByApplicationIdAndJobType(application.id(), WorkflowJobType.CREDENTIAL_RECONCILIATION)
                .orElseGet(() -> workItemRepository.save(OnboardingWorkItem.pending(application.id(), WorkflowJobType.CREDENTIAL_RECONCILIATION, now)));
    }

    private UUID reference(UUID applicationId, ProvisioningStepType type) { return UUID.fromString(stringReference(applicationId, type)); }
    private String stringReference(UUID applicationId, ProvisioningStepType type) {
        return stepRepository.findByApplicationIdAndStepType(applicationId, type)
                .filter(step -> step.status() == ProvisioningStepStatus.SUCCEEDED)
                .map(OnboardingProvisioningStep::externalReference)
                .orElseThrow(() -> new IllegalStateException("Required provisioning reference is missing: " + type));
    }
    private OnboardingApplication requireApplication(UUID id) {
        return applicationRepository.findById(id).orElseThrow(() -> new OnboardingApplicationNotFoundException(id));
    }
    private void saveHistory(OnboardingApplication previous, OnboardingApplication next, String reason, Instant now) {
        historyRepository.save(OnboardingStatusHistory.transition(previous.id(), previous.status(), next.status(), reason, OnboardingActorType.PROVISIONING, now));
    }
    private String fingerprintFor(OnboardingApplication application, ApplicantData applicant, ProvisioningStepType type) {
        String payload = switch (type) {
            case CREATE_CUSTOMER -> canonical(application.id(), application.email(), applicant.firstName(),
                    applicant.middleName(), applicant.lastName(), applicant.birthDate(), applicant.nationality(),
                    applicant.documentType(), applicant.documentNumber(), applicant.documentIssuingCountry(),
                    applicant.documentExpirationDate(), applicant.phoneNumber(), applicant.street(),
                    applicant.streetNumber(), applicant.city(), applicant.province(), applicant.postalCode(),
                    applicant.country());
            case APPROVE_CUSTOMER_KYC -> canonical(
                    reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER),
                    "AUTO_ONBOARDING_APPROVED", "onboarding-service");
            case OPEN_ACCOUNT -> canonical(
                    reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER), "SAVINGS", "ARS");
            case PRECREATE_KEYCLOAK_USER -> canonical(application.id(), application.email(),
                    applicant.firstName(), applicant.lastName(), "UPDATE_PROFILE", "UPDATE_PASSWORD");
            case CREATE_IDENTITY_LINK -> canonical(
                    reference(application.id(), ProvisioningStepType.CREATE_CUSTOMER),
                    stringReference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER), "KEYCLOAK");
            case ACTIVATE_ACCOUNT -> canonical(reference(application.id(), ProvisioningStepType.OPEN_ACCOUNT),
                    "AUTO_ONBOARDING_APPROVED", "onboarding-service");
            case SEND_CREDENTIAL_SETUP_EMAIL -> canonical(
                    stringReference(application.id(), ProvisioningStepType.PRECREATE_KEYCLOAK_USER), "PT24H");
        };
        return fingerprint(payload);
    }
    private String canonical(Object... values) {
        return java.util.Arrays.stream(values)
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining("|"));
    }
    private String fingerprint(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
