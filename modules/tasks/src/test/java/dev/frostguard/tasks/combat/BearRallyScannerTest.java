package dev.frostguard.tasks.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BearRallyScannerTest {

    @Test
    void returnsEmptyListWhenZeroJoinButtonsFound() {
        BearRallyScanner scanner = new BearRallyScanner(
                (template, config) -> List.of(),
                (tl, br) -> null
        );

        List<BearRallyCandidate> candidates = scanner.scanCandidates(Instant.now());
        assertTrue(candidates.isEmpty());
    }

    @Test
    void parsesAndSortsCandidatesFromTopToBottom() {
        ImageSearchResultData btnLower = ImageSearchResultData.hit(600, 600, 95.0);
        ImageSearchResultData btnUpper = ImageSearchResultData.hit(600, 300, 95.0);

        BearRallyScanner scanner = new BearRallyScanner(
                (template, config) -> List.of(btnLower, btnUpper),
                (tl, br) -> {
                    // Extract based on region
                    if (tl.getY() < 400) {
                        // Upper card
                        if (tl.getX() == 281) return "PlayerOne";
                        if (tl.getX() == 626) return "3/6";
                        if (tl.getX() == 284) return "50.0K/200.0K";
                        if (tl.getX() == 571) return "04:30";
                    } else {
                        // Lower card
                        if (tl.getX() == 281) return "PlayerTwo";
                        if (tl.getX() == 626) return "1/6";
                        if (tl.getX() == 284) return "100.0K/150.0K";
                        if (tl.getX() == 571) return "02:15";
                    }
                    return null;
                }
        );

        Instant now = Instant.parse("2026-08-19T10:00:00Z");
        List<BearRallyCandidate> candidates = scanner.scanCandidates(now);

        assertEquals(2, candidates.size());

        // Verify sorted from top to bottom
        BearRallyCandidate first = candidates.get(0);
        assertEquals("PlayerOne", first.hostName());
        assertEquals(3, first.currentMembers());
        assertEquals(6, first.maxMembers());
        assertEquals(200_000L, first.rallyCapacity());
        assertEquals(50_000L, first.remainingCapacity());
        assertEquals(150_000L, first.currentTroops());
        assertEquals(Duration.ofSeconds(270), first.countdown());

        BearRallyCandidate second = candidates.get(1);
        assertEquals("PlayerTwo", second.hostName());
        assertEquals(1, second.currentMembers());
        assertEquals(6, second.maxMembers());
        assertEquals(150_000L, second.rallyCapacity());
        assertEquals(100_000L, second.remainingCapacity());
        assertEquals(50_000L, second.currentTroops());
        assertEquals(Duration.ofSeconds(135), second.countdown());
    }
}
