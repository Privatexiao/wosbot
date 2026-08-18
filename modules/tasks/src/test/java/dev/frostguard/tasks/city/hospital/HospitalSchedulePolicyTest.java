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
}
