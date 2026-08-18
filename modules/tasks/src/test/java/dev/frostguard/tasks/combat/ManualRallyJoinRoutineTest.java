package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ManualRallyJoinRoutineTest {

    @Test
    void marchLimitStaysWithinAvailableFormationSlots() {
        assertEquals(1, ManualRallyJoinRoutine.normalizeMarchLimit(null));
        assertEquals(1, ManualRallyJoinRoutine.normalizeMarchLimit(0));
        assertEquals(4, ManualRallyJoinRoutine.normalizeMarchLimit(4));
        assertEquals(6, ManualRallyJoinRoutine.normalizeMarchLimit(9));
    }
}
