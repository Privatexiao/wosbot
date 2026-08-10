package dev.frostguard.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsInstallerHandoffTest {
    @TempDir
    Path temp;

    @Test
    void startsHiddenWaiterWithInstallerAndParentIdentity() throws Exception {
        Path installer = Files.writeString(temp.resolve("Frostguard-3.0.1.exe"), "test");
        AtomicReference<java.util.List<String>> command = new AtomicReference<>();
        AtomicReference<java.util.Map<String, String>> environment = new AtomicReference<>();
        WindowsInstallerHandoff handoff = new WindowsInstallerHandoff((actualCommand, actualEnvironment) -> {
            command.set(actualCommand);
            environment.set(actualEnvironment);
        });

        InstallerHandoff.HandoffSession session = handoff.stage(installer, 4242L);

        assertTrue(command.get().contains("Hidden"));
        assertTrue(command.get().getLast().contains("Get-Process -Id $targetPid"));
        assertEquals("4242", environment.get().get(WindowsInstallerHandoff.PID_ENV));
        assertEquals(installer.toAbsolutePath().normalize().toString(),
                environment.get().get(WindowsInstallerHandoff.INSTALLER_ENV));
        Path token = Path.of(environment.get().get(WindowsInstallerHandoff.TOKEN_PATH_ENV));
        assertTrue(command.get().getLast().contains("TOKEN_PATH"));
        session.authorize();
        assertEquals(environment.get().get(WindowsInstallerHandoff.TOKEN_VALUE_ENV), Files.readString(token));
        session.cancel();
        assertTrue(Files.notExists(token));
    }

    @Test
    void rejectsInvalidInputsBeforeStartingWaiter() {
        WindowsInstallerHandoff handoff = new WindowsInstallerHandoff((command, environment) -> {
            throw new AssertionError("Waiter should not start");
        });
        assertThrows(UpdateException.class, () -> handoff.stage(temp.resolve("missing.exe"), 10L));
    }

    @Test
    void reportsWaiterLaunchFailure() throws Exception {
        Path installer = Files.writeString(temp.resolve("Frostguard-3.0.1.exe"), "test");
        WindowsInstallerHandoff handoff = new WindowsInstallerHandoff((command, environment) -> {
            throw new IOException("blocked");
        });
        assertThrows(UpdateException.class, () -> handoff.stage(installer, 10L));
    }
}
