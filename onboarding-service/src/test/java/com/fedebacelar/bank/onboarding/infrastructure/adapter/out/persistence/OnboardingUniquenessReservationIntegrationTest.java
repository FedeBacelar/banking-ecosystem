package com.fedebacelar.bank.onboarding.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fedebacelar.bank.onboarding.TestcontainersConfiguration;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingApplicationRepositoryPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingUniquenessReservationPort;
import com.fedebacelar.bank.onboarding.application.port.out.OnboardingWorkItemRepositoryPort;
import com.fedebacelar.bank.onboarding.domain.enums.UniquenessReservationType;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobStatus;
import com.fedebacelar.bank.onboarding.domain.enums.WorkflowJobType;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingApplication;
import com.fedebacelar.bank.onboarding.domain.model.OnboardingWorkItem;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "onboarding.review.worker-batch-size=0",
        "onboarding.provisioning.worker-batch-size=0"
})
@Import(TestcontainersConfiguration.class)
class OnboardingUniquenessReservationIntegrationTest {
    @Autowired
    private OnboardingApplicationRepositoryPort applications;

    @Autowired
    private OnboardingUniquenessReservationPort reservations;

    @Autowired
    private OnboardingWorkItemRepositoryPort workItems;

    @Test
    void onlyOneApplicationCanReserveTheSameNormalizedIdentityAcrossConcurrentWorkers() throws Exception {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        OnboardingApplication first = applications.save(OnboardingApplication.start(
                "first@example.com", "magic-1", now.plusSeconds(1800), now.plusSeconds(86400), now));
        OnboardingApplication second = applications.save(OnboardingApplication.start(
                "second@example.com", "magic-2", now.plusSeconds(1800), now.plusSeconds(86400), now));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> firstAttempt = attempt(ready, start, first, now);
        Callable<Boolean> secondAttempt = attempt(ready, start, second, now);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(firstAttempt);
            var secondResult = executor.submit(secondAttempt);
            ready.await();
            start.countDown();

            assertThat(List.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder(true, false);
        }
    }

    @Test
    void onlyOneWorkerInstanceCanClaimTheSameDueWorkItem() throws Exception {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");
        OnboardingApplication application = applications.save(OnboardingApplication.start(
                "worker-race@example.com", "magic-worker", now.plusSeconds(1800), now.plusSeconds(86400), now));
        workItems.save(OnboardingWorkItem.pending(application.id(), WorkflowJobType.AUTO_REVIEW, now));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<OnboardingWorkItem>> claim = () -> {
            ready.countDown();
            start.await();
            return workItems.claimNext(WorkflowJobType.AUTO_REVIEW, now, Duration.ofMinutes(2));
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(claim);
            var second = executor.submit(claim);
            ready.await();
            start.countDown();

            Optional<OnboardingWorkItem> firstClaim = first.get();
            Optional<OnboardingWorkItem> secondClaim = second.get();
            assertThat(List.of(firstClaim.isPresent(), secondClaim.isPresent()))
                    .containsExactlyInAnyOrder(true, false);

            OnboardingWorkItem claimed = firstClaim.orElseGet(secondClaim::orElseThrow);
            assertThat(claimed.version()).isGreaterThan(0L);
            assertThat(workItems.save(claimed.succeed(now.plusSeconds(1))).status())
                    .isEqualTo(WorkflowJobStatus.SUCCEEDED);
        }
    }

    private Callable<Boolean> attempt(CountDownLatch ready, CountDownLatch start,
            OnboardingApplication application, Instant now) {
        return () -> {
            ready.countDown();
            start.await();
            return reservations.tryAcquire(
                    UniquenessReservationType.DOCUMENT, "DNI|AR|30111222", application.id(), now
            );
        };
    }
}
