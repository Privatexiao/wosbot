package dev.frostguard.tasks.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.frostguard.tasks.economy.GatherRoutine.ActiveGatherMarchCandidate;
import dev.frostguard.tasks.economy.GatherRoutine.GatherType;
import dev.frostguard.tasks.economy.GatherRoutine.OverflowRecallCandidate;
import dev.frostguard.tasks.economy.GatherRoutine.RecallAttempt;
import dev.frostguard.tasks.economy.GatherRoutine.RecallReason;

class GatherOverflowRecallPlanTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 23, 0);

    @Test
    void plansEachOverflowQueueOnlyOnceAcrossAllRecallReasons() {
        List<ActiveGatherMarchCandidate> active = List.of(
                candidate(GatherType.MEAT, 0, 180),
                candidate(GatherType.MEAT, 1, 120),
                candidate(GatherType.MEAT, 2, 60));

        List<OverflowRecallCandidate> plan = GatherRoutine.planOverflowRecalls(
                active, List.of(GatherType.COAL), 1);

        assertEquals(2, plan.size());
        assertEquals(List.of(0, 1), plan.stream()
                .map(item -> item.candidate().queueIndex())
                .toList());
        assertEquals(List.of(RecallReason.DISABLED_TYPE, RecallReason.DISABLED_TYPE), plan.stream()
                .map(OverflowRecallCandidate::reason)
                .toList());
    }

    @Test
    void prefersOneLongestDuplicateBeforeFallbackCandidates() {
        List<ActiveGatherMarchCandidate> active = List.of(
                candidate(GatherType.MEAT, 0, 180),
                candidate(GatherType.MEAT, 1, 60),
                candidate(GatherType.COAL, 2, 120));

        List<OverflowRecallCandidate> plan = GatherRoutine.planOverflowRecalls(
                active, List.of(GatherType.MEAT, GatherType.COAL), 2);

        assertEquals(1, plan.size());
        assertEquals(0, plan.getFirst().candidate().queueIndex());
        assertEquals(RecallReason.DUPLICATE_TYPE, plan.getFirst().reason());
    }

    @Test
    void returnsNoPlanWhenGatherCountIsWithinConfiguredLimit() {
        List<ActiveGatherMarchCandidate> active = List.of(
                candidate(GatherType.MEAT, 0, 180),
                candidate(GatherType.COAL, 1, 120));

        assertEquals(List.of(), GatherRoutine.planOverflowRecalls(
                active, List.of(GatherType.MEAT, GatherType.COAL), 2));
    }

    @Test
    void stopsAfterFirstScanWithoutRecallControls() {
        List<OverflowRecallCandidate> plan = List.of(
                new OverflowRecallCandidate(candidate(GatherType.MEAT, 0, 180), RecallReason.DISABLED_TYPE),
                new OverflowRecallCandidate(candidate(GatherType.MEAT, 1, 120), RecallReason.DISABLED_TYPE));
        AtomicInteger attempts = new AtomicInteger();

        var result = GatherRoutine.executeOverflowRecallPlan(plan, ignored -> {
            attempts.incrementAndGet();
            return RecallAttempt.CONTROLS_NOT_FOUND;
        });

        assertEquals(1, attempts.get());
        assertEquals(0, result.recalled());
        assertEquals(true, result.controlsMissing());
    }

    private ActiveGatherMarchCandidate candidate(GatherType type, int queueIndex, int minutes) {
        return new ActiveGatherMarchCandidate(type, queueIndex, NOW.plusMinutes(minutes));
    }
}
