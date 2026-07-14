package com.fedebacelar.bank.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

final class LocalOnboardingStartRateLimiter {

    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    private final Cache<String, ClientWindow> clients;
    private final Ticker ticker;
    private final int shortWindowRequests;
    private final long shortWindowNanos;
    private final int longWindowRequests;
    private final long longWindowNanos;
    private final int globalShortWindowRequests;
    private final int globalLongWindowRequests;
    private final ClientWindow globalWindow = new ClientWindow();

    LocalOnboardingStartRateLimiter(OnboardingStartRateLimitProperties properties, Ticker ticker) {
        this.ticker = ticker;
        this.shortWindowRequests = properties.shortWindowRequests();
        this.shortWindowNanos = properties.shortWindow().toNanos();
        this.longWindowRequests = properties.longWindowRequests();
        this.longWindowNanos = properties.longWindow().toNanos();
        this.globalShortWindowRequests = properties.globalShortWindowRequests();
        this.globalLongWindowRequests = properties.globalLongWindowRequests();
        this.clients = Caffeine.<String, ClientWindow>newBuilder()
                .maximumSize(properties.maximumClients())
                .expireAfterAccess(properties.clientIdleExpiration())
                .ticker(ticker)
                .build();
    }

    Decision acquire(String clientKey) {
        ClientWindow window = clients.get(clientKey, ignored -> new ClientWindow());
        long now = ticker.read();
        Decision clientDecision = window.acquire(
                now,
                shortWindowRequests,
                shortWindowNanos,
                longWindowRequests,
                longWindowNanos
        );
        if (!clientDecision.allowed()) {
            return clientDecision;
        }
        return globalWindow.acquire(
                now,
                globalShortWindowRequests,
                shortWindowNanos,
                globalLongWindowRequests,
                longWindowNanos
        );
    }

    long estimatedClientCount() {
        clients.cleanUp();
        return clients.estimatedSize();
    }

    record Decision(boolean allowed, long retryAfterSeconds) {

        private static Decision permit() {
            return new Decision(true, 0);
        }

        private static Decision rejected(long waitNanos) {
            long seconds = Math.max(1, Math.floorDiv(waitNanos - 1, NANOS_PER_SECOND) + 1);
            return new Decision(false, seconds);
        }
    }

    private static final class ClientWindow {

        private final Deque<Long> acceptedRequests = new ArrayDeque<>();

        private synchronized Decision acquire(
                long now,
                int shortLimit,
                long shortWindow,
                int longLimit,
                long longWindow
        ) {
            while (!acceptedRequests.isEmpty() && elapsed(now, acceptedRequests.peekFirst()) >= longWindow) {
                acceptedRequests.removeFirst();
            }

            int shortCount = 0;
            long firstShortRequest = 0;
            for (long acceptedAt : acceptedRequests) {
                if (elapsed(now, acceptedAt) < shortWindow) {
                    if (shortCount == 0) {
                        firstShortRequest = acceptedAt;
                    }
                    shortCount++;
                }
            }

            long shortWait = shortCount >= shortLimit
                    ? shortWindow - elapsed(now, firstShortRequest)
                    : 0;
            long longWait = acceptedRequests.size() >= longLimit
                    ? longWindow - elapsed(now, acceptedRequests.peekFirst())
                    : 0;
            long wait = Math.max(shortWait, longWait);
            if (wait > 0) {
                return Decision.rejected(wait);
            }

            acceptedRequests.addLast(now);
            return Decision.permit();
        }

        private long elapsed(long now, long then) {
            return Math.max(0, now - then);
        }
    }
}
