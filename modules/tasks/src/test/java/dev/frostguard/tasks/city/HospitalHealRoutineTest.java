package dev.frostguard.tasks.city;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import org.junit.jupiter.api.Test;

class HospitalHealRoutineTest {

    @Test
    void disabledRoutineReturnsBeforeAnyEmulatorInteraction() {
        AccountDescriptor profile = new AccountDescriptor(1L, "test-profile", "test-device",
                true, 1L, 30L);
        profile.setConfig(ConfigurationKeyEnum.HOSPITAL_HEAL_ENABLED_BOOL, false);
        HospitalHealRoutine routine = new HospitalHealRoutine(profile, TpDailyTaskEnum.HOSPITAL_HEAL);

        assertDoesNotThrow(routine::execute);
    }
}
