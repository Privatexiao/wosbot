package dev.frostguard.tasks.combat;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.PointData;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BearRallyDecisionPolicyTest {

    @Test
    public void testDisabledAdvancedJoinUsesDefaultPath() {
        AccountDescriptor account = new AccountDescriptor(1L);
        account.setConfig(ConfigurationKeyEnum.BEAR_TRAP_ADVANCED_JOIN_ENABLED_BOOL, false);

        BearRallyCandidate candidate = new BearRallyCandidate(
                new PointData(100, 100), new AreaData(new PointData(0, 0), new PointData(200, 200)),
                "Host1", 1L, 6L, Duration.ofMinutes(4), true);

        BearRallyDecisionPolicy.Decision decision = BearRallyDecisionPolicy.evaluate(
                candidate, account, LocalDateTime.now(), Clock.systemUTC());

        assertEquals(BearRallyDecisionPolicy.DecisionResult.JOIN, decision.result());
        assertFalse(decision.frenzyActive());
    }

    @Test
    public void testFrenzyModeBypassesMemberThreshold() {
        AccountDescriptor account = new AccountDescriptor(1L);
        account.setConfig(ConfigurationKeyEnum.BEAR_TRAP_ADVANCED_JOIN_ENABLED_BOOL, true);
        account.setConfig(ConfigurationKeyEnum.BEAR_TRAP_FRENZY_MODE_ENABLED_BOOL, true);
        account.setConfig(ConfigurationKeyEnum.BEAR_TRAP_FRENZY_START_MINUTE_INT, 22);
        account.setConfig(ConfigurationKeyEnum.BEAR_TRAP_MIN_MEMBER_COUNT_INT, 5);

        Instant fixedNow = Instant.parse("2026-08-14T10:25:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneId.of("UTC"));
        LocalDateTime trapStartTime = LocalDateTime.ofInstant(Instant.parse("2026-08-14T10:00:00Z"), ZoneId.of("UTC"));

        BearRallyCandidate candidate = new BearRallyCandidate(
                new PointData(100, 100), new AreaData(new PointData(0, 0), new PointData(200, 200)),
                "Host1", 1L, 6L, Duration.ofMinutes(4), true);

        BearRallyDecisionPolicy.Decision decision = BearRallyDecisionPolicy.evaluate(
                candidate, account, trapStartTime, fixedClock);

        assertEquals(BearRallyDecisionPolicy.DecisionResult.JOIN, decision.result());
        assertTrue(decision.frenzyActive());
    }
}
