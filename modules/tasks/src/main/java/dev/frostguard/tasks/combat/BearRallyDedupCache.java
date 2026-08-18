package dev.frostguard.tasks.combat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe candidate deduplication TTL cache for Bear Trap rally join requests.
 */
public class BearRallyDedupCache {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(300);
    private final Map<String, Instant> cache = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public BearRallyDedupCache() {
        this(Clock.systemUTC(), DEFAULT_TTL);
    }

    public BearRallyDedupCache(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public boolean isDuplicate(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }

        cleanExpired();
        Instant expiry = cache.get(key);
        if (expiry == null) {
            return false;
        }

        if (!clock.instant().isBefore(expiry)) {
            cache.remove(key);
            return false;
        }

        return true;
    }

    public void markJoined(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        cache.put(key, clock.instant().plus(ttl));
    }

    public void clear() {
        cache.clear();
    }

    private void cleanExpired() {
        Instant now = clock.instant();
        cache.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
    }
}
