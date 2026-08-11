package dev.frostguard.app.panel.update;

import dev.frostguard.update.InstallerHandoff;
import dev.frostguard.update.UpdateException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateExitCoordinatorTest {
    @Test
    void authorizesThenShutsDownAndExits() throws Exception {
        Session session = new Session();
        AtomicBoolean shutdown = new AtomicBoolean();
        AtomicBoolean exited = new AtomicBoolean();
        UpdateExitCoordinator coordinator = new UpdateExitCoordinator(() -> shutdown.set(true),
                () -> exited.set(true), () -> { throw new AssertionError("Failure exit should not run"); });

        coordinator.execute(session);

        assertTrue(session.authorized);
        assertTrue(shutdown.get());
        assertTrue(exited.get());
        assertFalse(session.cancelled);
    }

    @Test
    void cancelsHandoffAndExitsWhenShutdownFails() {
        Session session = new Session();
        AtomicBoolean failedExit = new AtomicBoolean();
        UpdateExitCoordinator coordinator = new UpdateExitCoordinator(
                () -> { throw new IllegalStateException("database busy"); },
                () -> { throw new AssertionError("Success exit should not run"); },
                () -> failedExit.set(true));

        assertThrows(IllegalStateException.class, () -> coordinator.execute(session));
        assertTrue(session.authorized);
        assertTrue(session.cancelled);
        assertTrue(failedExit.get());
    }

    private static final class Session implements InstallerHandoff.HandoffSession {
        private boolean authorized;
        private boolean cancelled;

        @Override
        public void authorize() throws UpdateException {
            authorized = true;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
