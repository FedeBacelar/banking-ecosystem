package com.fedebacelar.bank.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class LocalOnboardingStartRateLimiterTest {

    @Test
    void shouldApplyTheShortSlidingWindowAndReturnExactRetryAfter() {
        MutableTicker ticker = new MutableTicker();
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(properties(), ticker);

        assertThat(limiter.acquire("client-a").allowed()).isTrue();
        assertThat(limiter.acquire("client-a").allowed()).isTrue();
        assertThat(limiter.acquire("client-a").allowed()).isTrue();
        assertThat(limiter.acquire("client-a"))
                .isEqualTo(new LocalOnboardingStartRateLimiter.Decision(false, 60));

        ticker.advance(Duration.ofSeconds(59));
        assertThat(limiter.acquire("client-a").retryAfterSeconds()).isEqualTo(1);

        ticker.advance(Duration.ofSeconds(1));
        assertThat(limiter.acquire("client-a").allowed()).isTrue();
    }

    @Test
    void shouldApplyTheLongWindowWithoutExtendingItOnRejectedRequests() {
        MutableTicker ticker = new MutableTicker();
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(properties(), ticker);

        for (int request = 0; request < 10; request++) {
            assertThat(limiter.acquire("client-a").allowed()).isTrue();
            ticker.advance(Duration.ofMinutes(1));
        }

        assertThat(limiter.acquire("client-a").retryAfterSeconds()).isEqualTo(50 * 60);
        ticker.advance(Duration.ofMinutes(49));
        assertThat(limiter.acquire("client-a").retryAfterSeconds()).isEqualTo(60);
        ticker.advance(Duration.ofMinutes(1));
        assertThat(limiter.acquire("client-a").allowed()).isTrue();
    }

    @Test
    void shouldKeepIndependentClientWindows() {
        MutableTicker ticker = new MutableTicker();
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(properties(), ticker);

        for (int request = 0; request < 3; request++) {
            assertThat(limiter.acquire("client-a").allowed()).isTrue();
        }

        assertThat(limiter.acquire("client-a").allowed()).isFalse();
        assertThat(limiter.acquire("client-b").allowed()).isTrue();
    }

    @Test
    void shouldNeverExceedTheWindowUnderConcurrentRequests() throws Exception {
        MutableTicker ticker = new MutableTicker();
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(properties(), ticker);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(16)) {
            List<java.util.concurrent.Future<Boolean>> results = java.util.stream.IntStream.range(0, 32)
                    .mapToObj(ignored -> executor.submit(() -> {
                        start.await();
                        return limiter.acquire("same-client").allowed();
                    }))
                    .toList();
            start.countDown();

            long allowed = 0;
            for (var result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    allowed++;
                }
            }
            assertThat(allowed).isEqualTo(3);
        }
    }

    @Test
    void shouldBoundAndExpireClientState() {
        MutableTicker ticker = new MutableTicker();
        OnboardingStartRateLimitProperties properties = properties(2, Duration.ofHours(1));
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(properties, ticker);

        limiter.acquire("client-a");
        limiter.acquire("client-b");
        limiter.acquire("client-c");
        assertThat(limiter.estimatedClientCount()).isLessThanOrEqualTo(2);

        ticker.advance(Duration.ofHours(2));
        assertThat(limiter.estimatedClientCount()).isZero();
    }

    @Test
    void shouldKeepAProcessWideLimitEvenWhenClientBucketsAreEvicted() {
        MutableTicker ticker = new MutableTicker();
        LocalOnboardingStartRateLimiter limiter = new LocalOnboardingStartRateLimiter(
                properties(2, Duration.ofHours(2)),
                ticker
        );

        for (int request = 0; request < 30; request++) {
            assertThat(limiter.acquire("client-" + request).allowed()).isTrue();
        }

        assertThat(limiter.estimatedClientCount()).isLessThanOrEqualTo(2);
        assertThat(limiter.acquire("new-client"))
                .isEqualTo(new LocalOnboardingStartRateLimiter.Decision(false, 60));
    }

    private OnboardingStartRateLimitProperties properties() {
        return properties(50_000, Duration.ofHours(2));
    }

    private OnboardingStartRateLimitProperties properties(long maximumClients, Duration expiration) {
        return new OnboardingStartRateLimitProperties(
                true,
                3,
                Duration.ofMinutes(1),
                10,
                Duration.ofHours(1),
                30,
                300,
                maximumClients,
                expiration,
                64,
                ""
        );
    }

    private static final class MutableTicker implements Ticker {

        private final AtomicLong value = new AtomicLong();

        @Override
        public long read() {
            return value.get();
        }

        private void advance(Duration duration) {
            value.addAndGet(duration.toNanos());
        }
    }
}
