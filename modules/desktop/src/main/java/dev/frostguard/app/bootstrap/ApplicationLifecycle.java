package dev.frostguard.app.bootstrap;

import dev.frostguard.app.panel.misc.GiftCodeAutomationService;
import dev.frostguard.data.access.DataStore;
import dev.frostguard.engine.service.AnalyticsService;
import dev.frostguard.engine.service.CustomTaskService;
import dev.frostguard.engine.service.ScheduleService;
import dev.frostguard.engine.service.TelegramBotService;
import dev.frostguard.vision.logging.ProfileContextLogger;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ApplicationLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationLifecycle.class);
    private static final AtomicBoolean SHUTDOWN_ACTIVE = new AtomicBoolean();
    private static final RuntimeShutdownCoordinator COORDINATOR = new RuntimeShutdownCoordinator(List.of(
            new RuntimeShutdownCoordinator.Step("scheduler", () -> ScheduleService.obtain().haltEngine()),
            new RuntimeShutdownCoordinator.Step("gift code automation",
                    () -> GiftCodeAutomationService.getInstance().shutdown()),
            new RuntimeShutdownCoordinator.Step("Telegram command server",
                    () -> TelegramBotService.getInstance().stop()),
            new RuntimeShutdownCoordinator.Step("custom task loader", () -> CustomTaskService.getInstance().shutdown()),
            new RuntimeShutdownCoordinator.Step("analytics", () -> AnalyticsService.getInstance().trackAppShutdown()),
            new RuntimeShutdownCoordinator.Step("persistence", () -> DataStore.getInstance().close()),
            new RuntimeShutdownCoordinator.Step("ADB", ApplicationLifecycle::terminateAdbProcess),
            new RuntimeShutdownCoordinator.Step("profile logging", ProfileContextLogger::shutdown),
            new RuntimeShutdownCoordinator.Step("workspace", Main::closeWorkspace)
    ));

    private ApplicationLifecycle() {
    }

    public static void stopForUpdate() throws LifecycleException {
        if (!SHUTDOWN_ACTIVE.compareAndSet(false, true)) {
            throw new LifecycleException("Another shutdown is already active");
        }
        try {
            COORDINATOR.shutdown();
        } catch (RuntimeShutdownCoordinator.ShutdownException exception) {
            SHUTDOWN_ACTIVE.set(false);
            throw new LifecycleException(exception.getMessage(), exception);
        }
    }

    public static void exitNormally(int status) {
        if (SHUTDOWN_ACTIVE.compareAndSet(false, true)) {
            try {
                COORDINATOR.shutdown();
            } catch (RuntimeShutdownCoordinator.ShutdownException exception) {
                LOG.warn("Runtime shutdown completed with errors: {}", exception.getMessage());
            }
        }
        Platform.exit();
        System.exit(status);
    }

    public static void exitAfterUpdateHandoff() {
        Platform.exit();
        System.exit(0);
    }

    static void runShutdownHook() {
        if (!SHUTDOWN_ACTIVE.compareAndSet(false, true)) {
            return;
        }
        try {
            COORDINATOR.shutdown();
        } catch (RuntimeShutdownCoordinator.ShutdownException exception) {
            LOG.warn("Shutdown hook completed with errors: {}", exception.getMessage());
        }
    }

    private static void terminateAdbProcess() throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
            return;
        }
        new ProcessBuilder("taskkill", "/F", "/IM", "adb.exe").start();
        LOG.info("adb.exe shutdown requested");
    }

    public static final class LifecycleException extends Exception {
        LifecycleException(String message) {
            super(message);
        }

        LifecycleException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
