package dev.frostguard.tasks.city.hospital;

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
            return totalWoundedCount; // If no help available, just heal all at once (or base it on other rules).
        }

        long targetHealTimeSeconds = allianceHelpCount * reductionSecondsPerHelp;
        
        // Calculate average time per troop
        double secondsPerTroop = (double) totalHealTimeSeconds / totalWoundedCount;
        
        if (secondsPerTroop <= 0) {
            return -1;
        }

        int targetTroopCount = (int) Math.floor(targetHealTimeSeconds / secondsPerTroop);

        // Clamp between 1 and totalWoundedCount
        if (targetTroopCount < 1) {
            return 1;
        }
        if (targetTroopCount > totalWoundedCount) {
            return totalWoundedCount;
        }

        return targetTroopCount;
    }
}
