package dev.frostguard.tasks.city.hospital;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class HospitalSchedulePolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 18, 12, 0);

    @Test
    void appliesBackoffToRecognitionFailure() {
        assertEquals(NOW.plusMinutes(15), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE, null));
    }

    @Test
    void schedulesActiveHealAfterRemainingTimeAndBuffer() {
        assertEquals(NOW.plusMinutes(10).plusSeconds(30), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, Duration.ofMinutes(10)));
    }

    @Test
    void usesConservativeFallbackWhenActiveDurationIsUnknown() {
        assertEquals(NOW.plusMinutes(15), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, null));
    }

    @Test
    void rejectsImplausiblyLargeActiveDuration() {
        assertEquals(NOW.plusMinutes(15), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, Duration.ofDays(31)));
    }

    @Test
    void unsupportedConfigurationDoesNotHotLoop() {
        assertEquals(NOW.plusHours(1), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.CONFIGURATION_UNSUPPORTED, null));
    }

    @Test
    void appliesExactBackoffsToEveryTerminalOutcome() {
        assertEquals(NOW.plusMinutes(5), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.NO_ENTRY, null));
        assertEquals(NOW.plusMinutes(30), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.NO_WOUNDED, null));
        assertEquals(NOW.plusMinutes(30), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.COMPLETED, null));
    }

    @Test
    void rejectsZeroAndNegativeActiveDurations() {
        assertEquals(NOW.plusMinutes(15), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, Duration.ZERO));
        assertEquals(NOW.plusMinutes(15), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, Duration.ofSeconds(-1)));
    }

    @Test
    void acceptsTheMaximumTrustedDurationAndAddsBuffer() {
        assertEquals(NOW.plusDays(30).plusSeconds(30), HospitalSchedulePolicy.nextRun(
                NOW, HospitalSchedulePolicy.Outcome.ACTIVE_HEAL, Duration.ofDays(30)));
    }

    @Test
    void capsUnrepresentableFutureTimesInsteadOfThrowing() {
        assertEquals(LocalDateTime.MAX, HospitalSchedulePolicy.nextRun(
                LocalDateTime.MAX.minusMinutes(1),
                HospitalSchedulePolicy.Outcome.CONFIGURATION_UNSUPPORTED, null));
    }

    @Test
    void treatsMaximumWaitAsAnOverflowSafeWarningThreshold() {
        assertEquals(true, HospitalSchedulePolicy.exceedsWarningThreshold(
                Duration.ofMinutes(31), 30));
        assertEquals(false, HospitalSchedulePolicy.exceedsWarningThreshold(
                Duration.ofMinutes(30), 30));
        assertEquals(false, HospitalSchedulePolicy.exceedsWarningThreshold(
                Duration.ofDays(30), Integer.MAX_VALUE));
        assertEquals(false, HospitalSchedulePolicy.exceedsWarningThreshold(
                Duration.ofMinutes(31), 0));
    }
}
