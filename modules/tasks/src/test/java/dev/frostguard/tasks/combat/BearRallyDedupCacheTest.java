package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

        cache.markJoined("host-1");
        assertTrue(cache.isDuplicate("host-1"));

        clock.instant = start.plus(Duration.ofMinutes(5));
        assertFalse(cache.isDuplicate("host-1"));
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
