package dev.frostguard.app.panel.update;

import dev.frostguard.update.InstallerHandoff;

final class UpdateExitCoordinator {
    private final ShutdownAction shutdown;
    private final Runnable exit;

    UpdateExitCoordinator(ShutdownAction shutdown, Runnable exit) {
        this.shutdown = shutdown;
        this.exit = exit;
    }

    void execute(InstallerHandoff.HandoffSession session) throws Exception {
        session.authorize();
        try {
            shutdown.run();
        } catch (Exception exception) {
            session.cancel();
            throw exception;
        }
        exit.run();
    }

    @FunctionalInterface
    interface ShutdownAction {
        void run() throws Exception;
    }
}
