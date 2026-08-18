package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BeastSlayRoutineTest {

    @Test
    void maximumAttacksAlsoRespectsAvailableQueueLimit() {
        assertEquals(3, BeastSlayRoutine.resolveAttackLimit(3, 10));
        assertEquals(2, BeastSlayRoutine.resolveAttackLimit(6, 2));
        assertEquals(6, BeastSlayRoutine.resolveAttackLimit(6, 0));
        assertEquals(0, BeastSlayRoutine.resolveAttackLimit(-1, 0));
    }

    @Test
    void negativeMaximumIsClampedAndMissingValueUsesDefault() {
        assertEquals(10, BeastSlayRoutine.normalizeAttackLimit(null));
        assertEquals(0, BeastSlayRoutine.normalizeAttackLimit(-5));
    }
}
