package com.dbwb.platform.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed-window request counter, keyed by whatever the caller decides
 * identifies the requester (an IP for anonymous endpoints, an account id for
 * authenticated ones).
 *
 * In memory, and therefore per instance. On a single node - which is what this
 * platform runs on today - that is the whole story. Behind more than one node
 * the effective ceiling multiplies by the node count, so this must move to a
 * shared store (Redis) before scaling out; it is deliberately a small,
 * dependency-free piece so that swap is easy.
 *
 * Fixed windows rather than a sliding log: a burst can straddle a boundary and
 * briefly get through up to twice the limit. That is an accepted trade for
 * O(1) memory per key, because the point here is to stop sustained automated
 * abuse, not to police an exact per-second rate.
 */
@Component
public class RateLimiter {

    /** Counters are dropped once their window has passed; this bounds how much dead weight can pile up first. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(Instant expiresAt, AtomicInteger count) {
        boolean hasExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    /**
     * Records one request against the key and says whether it is allowed.
     * Returns false once the key has spent its allowance for the window.
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = Instant.now();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(existing -> existing.hasExpired(now));
        }

        Window current = windows.compute(key, (ignored, existing) ->
                existing == null || existing.hasExpired(now)
                        ? new Window(now.plus(window), new AtomicInteger(0))
                        : existing);

        return current.count().incrementAndGet() <= limit;
    }

    /** Drops every counter. For tests, so one case cannot exhaust another's allowance. */
    public void reset() {
        windows.clear();
    }
}
