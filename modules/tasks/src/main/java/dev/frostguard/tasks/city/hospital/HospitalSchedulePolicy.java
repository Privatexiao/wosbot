package dev.frostguard.tasks.city.hospital;

import java.time.Duration;
import java.time.DateTimeException;
import java.time.LocalDateTime;

public final class HospitalSchedulePolicy {

    private static final Duration UNKNOWN_ACTIVE_HEAL_RETRY = Duration.ofMinutes(15);
    private static final Duration MAX_TRUSTED_ACTIVE_HEAL_TIME = Duration.ofDays(30);

    public enum Outcome {
        NO_ENTRY,
        NO_WOUNDED,
        CONFIGURATION_UNSUPPORTED,
        RECOGNITION_FAILURE,
        ACTIVE_HEAL,
        COMPLETED
    }

    private HospitalSchedulePolicy() {}

    public static LocalDateTime nextRun(LocalDateTime now, Outcome outcome, Duration remaining) {
        if (now == null || outcome == null) {
            throw new IllegalArgumentException("Current time and outcome are required");
        }
        Duration delay = switch (outcome) {
            case NO_ENTRY -> Duration.ofMinutes(5);
            case NO_WOUNDED, COMPLETED -> Duration.ofMinutes(30);
            case CONFIGURATION_UNSUPPORTED -> Duration.ofHours(1);
            case RECOGNITION_FAILURE -> Duration.ofMinutes(15);
            case ACTIVE_HEAL -> safeRemainingDelay(remaining);
        };
        return safeFutureTime(now, delay);
    }

    public static boolean exceedsWarningThreshold(Duration remaining, int warningMinutes) {
        if (remaining == null || remaining.isNegative() || warningMinutes <= 0) {
            return false;
        }
        return remaining.compareTo(Duration.ofMinutes((long) warningMinutes)) > 0;
    }

    private static Duration safeRemainingDelay(Duration remaining) {
        if (remaining == null || remaining.isNegative() || remaining.isZero()
                || remaining.compareTo(MAX_TRUSTED_ACTIVE_HEAL_TIME) > 0) {
            return UNKNOWN_ACTIVE_HEAL_RETRY;
        }
        try {
            Duration withBuffer = remaining.plusSeconds(30);
            return withBuffer.compareTo(Duration.ofMinutes(1)) < 0 ? Duration.ofMinutes(1) : withBuffer;
        } catch (ArithmeticException overflow) {
            return UNKNOWN_ACTIVE_HEAL_RETRY;
        }
    }

    private static LocalDateTime safeFutureTime(LocalDateTime now, Duration delay) {
        try {
            return now.plus(delay);
        } catch (DateTimeException | ArithmeticException invalidFutureTime) {
            return LocalDateTime.MAX;
        }
    }
}
