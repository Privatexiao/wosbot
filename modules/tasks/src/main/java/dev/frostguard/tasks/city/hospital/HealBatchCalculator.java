package dev.frostguard.tasks.city.hospital;

import java.math.BigInteger;

public class HealBatchCalculator {
    private final int totalWoundedCount;
    private final long totalHealTimeSeconds;
    private final int allianceHelpCount;
    private final long reductionSecondsPerHelp;

    public HealBatchCalculator(int totalWoundedCount, long totalHealTimeSeconds, int allianceHelpCount, long reductionSecondsPerHelp) {
        this.totalWoundedCount = totalWoundedCount;
        this.totalHealTimeSeconds = totalHealTimeSeconds;
        this.allianceHelpCount = allianceHelpCount;
        this.reductionSecondsPerHelp = reductionSecondsPerHelp;
    }

    /**
     * Calculates the optimal batch size based on the alliance help reduction.
     * @return the number of troops to heal in this batch, or -1 if inputs are invalid.
     */
    public int calculateBatchSize() {
        if (totalWoundedCount <= 0 || totalHealTimeSeconds <= 0) {
            return -1;
        }

        if (allianceHelpCount <= 0 || reductionSecondsPerHelp <= 0) {
            return -1;
        }

        final long targetHealTimeSeconds;
        try {
            targetHealTimeSeconds = Math.multiplyExact((long) allianceHelpCount, reductionSecondsPerHelp);
        } catch (ArithmeticException overflow) {
            return -1;
        }

        BigInteger numerator = BigInteger.valueOf(targetHealTimeSeconds)
                .multiply(BigInteger.valueOf(totalWoundedCount));
        BigInteger targetTroopCount = numerator.divide(BigInteger.valueOf(totalHealTimeSeconds));

        // Clamp between 1 and totalWoundedCount
        if (targetTroopCount.signum() < 1) {
            return 1;
        }
        if (targetTroopCount.compareTo(BigInteger.valueOf(totalWoundedCount)) > 0) {
            return totalWoundedCount;
        }

        return targetTroopCount.intValueExact();
    }
}
