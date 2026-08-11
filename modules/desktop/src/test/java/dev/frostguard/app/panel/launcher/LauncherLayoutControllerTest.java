package dev.frostguard.app.panel.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.frostguard.api.runtime.RuntimeChannel;
import org.junit.jupiter.api.Test;

class LauncherLayoutControllerTest {

    @Test
    void formatsUptimeFromElapsedSeconds() {
        assertEquals("00:00:00", LauncherLayoutController.formatUptime(0));
        assertEquals("01:01:01", LauncherLayoutController.formatUptime(3_661));
    }

    @Test
    void keepsHoursBeyondOneDay() {
        assertEquals("25:00:00", LauncherLayoutController.formatUptime(90_000));
    }

    @Test
    void identifiesRuntimeChannelInWindowTitle() {
        assertEquals("Frostguard v2.1.0 - Main [Stamina: 77]",
                LauncherLayoutController.formatWindowTitle(RuntimeChannel.STABLE, "2.1.0", "Main", 77));
        assertEquals("Frostguard Nightly v2.1.0 - Main [Stamina: 77]",
                LauncherLayoutController.formatWindowTitle(RuntimeChannel.NIGHTLY, "2.1.0", "Main", 77));
        assertEquals("Frostguard Development v2.1.0 - Main [Stamina: 77]",
                LauncherLayoutController.formatWindowTitle(RuntimeChannel.DEVELOPMENT, "2.1.0", "Main", 77));
    }
}
