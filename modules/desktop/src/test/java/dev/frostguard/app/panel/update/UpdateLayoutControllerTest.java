package dev.frostguard.app.panel.update;

import dev.frostguard.api.runtime.RuntimeChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateLayoutControllerTest {
    @Test
    void formatsInstallerSizesForReview() {
        assertEquals("512 B", UpdateLayoutController.formatSize(512));
        assertEquals("1.5 KiB", UpdateLayoutController.formatSize(1536));
        assertEquals("273.5 MiB", UpdateLayoutController.formatSize(286_739_610));
    }

    @Test
    void offersChannelSwitchingForInstalledAndPrChannelBuilds() {
        assertTrue(UpdateLayoutController.supportsChannelSwitch(RuntimeChannel.STABLE));
        assertTrue(UpdateLayoutController.supportsChannelSwitch(RuntimeChannel.NIGHTLY));
        assertFalse(UpdateLayoutController.supportsChannelSwitch(RuntimeChannel.DEVELOPMENT));
    }
}
