package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fedebacelar.bank.onboarding.TestcontainersConfiguration;
import com.fedebacelar.bank.onboarding.application.port.out.CredentialInvitationDeliveryRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.model.CredentialInvitationDelivery;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@SpringBootTest(properties = {
        "onboarding.review.worker-batch-size=0",
        "onboarding.provisioning.worker-batch-size=0"
})
@Import(TestcontainersConfiguration.class)
class CredentialInvitationDeliveryPersistenceIntegrationTest {
    @Autowired
    private OnboardingApplicationRepositoryPort applications;

    @Autowired
    private CredentialInvitationDeliveryRepositoryPort deliveries;

    @Test
    void onlyOneWorkerCanClaimAnInvitationAndAStaleLeaseOwnerIsFenced() throws Exception {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        OnboardingApplication application = saveApplication(now);
        deliveries.save(CredentialInvitationDelivery.pending(
                application.id(), "a".repeat(64), now
        ));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<CredentialInvitationDelivery>> claim = () -> {
            ready.countDown();
            start.await();
            return deliveries.claimNext(now, Duration.ofMinutes(1));
        };

        CredentialInvitationDelivery firstOwner;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(claim);
            var second = executor.submit(claim);
            ready.await();
            start.countDown();

            Optional<CredentialInvitationDelivery> firstClaim = first.get();
            Optional<CredentialInvitationDelivery> secondClaim = second.get();
            assertThat(List.of(firstClaim.isPresent(), secondClaim.isPresent()))
                    .containsExactlyInAnyOrder(true, false);
            firstOwner = firstClaim.orElseGet(secondClaim::orElseThrow);
        }

        CredentialInvitationDelivery secondOwner = deliveries.claimNext(
                now.plus(Duration.ofMinutes(2)), Duration.ofMinutes(5)
        ).orElseThrow();

        assertThat(secondOwner.version()).isGreaterThan(firstOwner.version());
        assertThatThrownBy(() -> deliveries.save(firstOwner.succeed(now.plusSeconds(121))))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(deliveries.save(secondOwner.succeed(now.plusSeconds(122))).status())
                .isEqualTo(WorkflowJobStatus.SUCCEEDED);
    }

    @Test
    void enforcesIdempotencyForEachApplicationAtTheDatabaseBoundary() {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        OnboardingApplication application = saveApplication(now);
        String keyHash = "b".repeat(64);
        deliveries.save(CredentialInvitationDelivery.pending(application.id(), keyHash, now));

        assertThatThrownBy(() -> deliveries.save(
                CredentialInvitationDelivery.pending(application.id(), keyHash, now.plusSeconds(1))
        )).isInstanceOf(DataIntegrityViolationException.class);

        CredentialInvitationDelivery claimed = deliveries.claimNext(
                now.plusSeconds(2), Duration.ofMinutes(1)
        ).orElseThrow();
        deliveries.save(claimed.succeed(now.plusSeconds(3)));
    }

    private OnboardingApplication saveApplication(Instant now) {
        String suffix = UUID.randomUUID().toString();
        return applications.save(OnboardingApplication.start(
                suffix + "@example.com",
                "magic-" + suffix,
                now.plusSeconds(1800),
                now.plusSeconds(86400),
                now
        ));
    }
}
