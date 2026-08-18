package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BearRallyDedupCacheTest {

    @Test
    void entryExpiresAtExactTtlBoundary() {
        Instant start = Instant.parse("2026-08-18T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        BearRallyDedupCache cache = new BearRallyDedupCache(clock, Duration.ofMinutes(5));
        BearRallyDedupCache.Scope scope = new BearRallyDedupCache.Scope("profile-1", "trap-1");

        cache.markJoined(scope, "host-1");
        assertTrue(cache.isDuplicate(scope, "host-1"));

        clock.instant = start.plus(Duration.ofMinutes(5));
        assertFalse(cache.isDuplicate(scope, "host-1"));
    }

    @Test
    void scopesEntriesByProfileAndActivity() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-18T10:00:00Z"));
        BearRallyDedupCache cache = new BearRallyDedupCache(clock, Duration.ofMinutes(5));
        BearRallyDedupCache.Scope first = new BearRallyDedupCache.Scope("profile-1", "trap-1");
        BearRallyDedupCache.Scope second = new BearRallyDedupCache.Scope("profile-1", "trap-2");

        cache.markJoined(first, "candidate");

        assertTrue(cache.isDuplicate(first, "candidate"));
        assertFalse(cache.isDuplicate(second, "candidate"));
    }

    @Test
    void clearsConservativelyWhenClockMovesBackward() {
        Instant start = Instant.parse("2026-08-18T10:00:00Z");
        MutableClock clock = new MutableClock(start);
        BearRallyDedupCache cache = new BearRallyDedupCache(clock, Duration.ofMinutes(5));
        BearRallyDedupCache.Scope scope = new BearRallyDedupCache.Scope("profile-1", "trap-1");
        cache.markJoined(scope, "candidate");

        clock.instant = start.minusSeconds(1);

        assertFalse(cache.isDuplicate(scope, "candidate"));
    }

    @Test
    void enforcesMaximumEntryCount() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-18T10:00:00Z"));
        BearRallyDedupCache cache = new BearRallyDedupCache(clock, Duration.ofMinutes(5), 2);
        BearRallyDedupCache.Scope scope = new BearRallyDedupCache.Scope("profile-1", "trap-1");

        cache.markJoined(scope, "one");
        clock.instant = clock.instant.plusSeconds(1);
        cache.markJoined(scope, "two");
        clock.instant = clock.instant.plusSeconds(1);
        cache.markJoined(scope, "three");

        assertEquals(2, cache.size());
        assertFalse(cache.isDuplicate(scope, "one"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
