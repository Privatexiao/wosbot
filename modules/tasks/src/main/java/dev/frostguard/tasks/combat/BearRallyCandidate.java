package dev.frostguard.tasks.combat;

import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.PointData;
import java.time.Duration;

/**
 * Parsed candidate card representation from the alliance rally list during Bear Trap event.
 */
public record BearRallyCandidate(
        PointData joinButtonPoint,
        AreaData cardArea,
        String hostName,
        long currentCount,
        long maxCount,
        Duration countdown,
        boolean isJoinable
) {
    /**
     * Unique candidate key for dedup caching.
     */
    public String getCandidateKey() {
        String cleanHost = hostName != null ? hostName.trim().toLowerCase() : "unknown";
        return cleanHost + ":" + currentCount + "/" + maxCount;
    }
}
