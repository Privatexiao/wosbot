package dev.frostguard.tasks.city.hospital;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HealBatchCalculatorTest {

    @Test
    void testNormalBatchCalculation() {
        // 100 troops, 3000 seconds total (30s per troop)
        // Alliance help: 10 count, 200s per help = 2000s target heal time
        // Expected batch size: floor(2000 / 30) = 66
        HealBatchCalculator calc = new HealBatchCalculator(100, 3000L, 10, 200L);
        assertEquals(66, calc.calculateBatchSize());
    }

    @Test
    void testTargetTimeExceedsTotalTime() {
        // 100 troops, 1000 seconds total (10s per troop)
        // Alliance help: 10 count, 200s per help = 2000s target heal time
        // Should clamp to max troops (100)
        HealBatchCalculator calc = new HealBatchCalculator(100, 1000L, 10, 200L);
        assertEquals(100, calc.calculateBatchSize());
    }

    @Test
    void testLessThanOneTroopTarget() {
        // 100 troops, 100000 seconds total (1000s per troop)
        // Alliance help: 1 count, 200s per help = 200s target heal time
        // floor(200 / 1000) = 0, should clamp to minimum 1
        HealBatchCalculator calc = new HealBatchCalculator(100, 100000L, 1, 200L);
        assertEquals(1, calc.calculateBatchSize());
    }

    @Test
    void testZeroWounded() {
        HealBatchCalculator calc = new HealBatchCalculator(0, 1000L, 10, 200L);
        assertEquals(-1, calc.calculateBatchSize());
    }

    @Test
    void testZeroTime() {
        HealBatchCalculator calc = new HealBatchCalculator(100, 0L, 10, 200L);
        assertEquals(-1, calc.calculateBatchSize());
    }

    @Test
    void testNoAllianceHelp() {
        HealBatchCalculator calc = new HealBatchCalculator(100, 1000L, 0, 0L);
        assertEquals(-1, calc.calculateBatchSize());
    }

    @Test
    void calculatesLargeValuesWithoutFloatingPointRounding() {
        HealBatchCalculator calc = new HealBatchCalculator(
                Integer.MAX_VALUE, Long.MAX_VALUE, 2, Long.MAX_VALUE / 4);
        assertEquals(1_073_741_823, calc.calculateBatchSize());
    }

    @Test
    void preservesLegacyFieldHospitalBatchCalculationWithoutWoundedCount() {
        assertEquals(105, HealBatchCalculator.calculateLegacyCompatibleBatchSize(30, 15, 210));
    }

    @Test
    void keepsLegacyMinimumBatchAtOne() {
        assertEquals(1, HealBatchCalculator.calculateLegacyCompatibleBatchSize(1_000, 1, 200));
    }

    @Test
    void rejectsInvalidLegacyCalibration() {
        assertEquals(-1, HealBatchCalculator.calculateLegacyCompatibleBatchSize(30, 0, 210));
    }
}
