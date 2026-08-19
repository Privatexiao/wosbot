package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ManualRallyJoinRoutineTest {

    @Test
    void marchLimitStaysWithinAvailableFormationSlots() {
        assertEquals(1, ManualRallyJoinRoutine.normalizeMarchLimit(null));
        assertEquals(1, ManualRallyJoinRoutine.normalizeMarchLimit(0));
        assertEquals(4, ManualRallyJoinRoutine.normalizeMarchLimit(4));
        assertEquals(6, ManualRallyJoinRoutine.normalizeMarchLimit(9));
    }

    @Test
    void deploymentRequiresPositiveScreenTransitionEvidence() {
        assertTrue(ManualRallyJoinRoutine.isDeploymentConfirmed(false, false, false));
        assertFalse(ManualRallyJoinRoutine.isDeploymentConfirmed(true, false, false));
        assertFalse(ManualRallyJoinRoutine.isDeploymentConfirmed(false, true, false));
        assertFalse(ManualRallyJoinRoutine.isDeploymentConfirmed(false, false, true));
    }
}
