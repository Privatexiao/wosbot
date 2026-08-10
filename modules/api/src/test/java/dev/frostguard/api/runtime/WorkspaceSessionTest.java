package dev.frostguard.api.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceSessionTest {
    @TempDir
    Path tempDir;

    private String previousWorkspace;
    private String previousLogDir;

    @BeforeEach
    void rememberRuntimeProperties() {
        previousWorkspace = System.getProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        previousLogDir = System.getProperty("frostguard.log.dir");
    }

    @AfterEach
    void restoreRuntimeProperties() {
        restore(WorkspacePaths.WORKSPACE_PROPERTY, previousWorkspace);
        restore("frostguard.log.dir", previousLogDir);
    }

    @Test
    void createsACompleteWorkspaceAndRejectsConcurrentUse() {
        WorkspacePaths paths = new WorkspacePaths(tempDir.resolve("bot-1"), RuntimeChannel.STABLE);

        try (WorkspaceSession ignored = WorkspaceSession.open(paths)) {
            assertTrue(paths.marker().toFile().isFile());
            assertTrue(paths.config().toFile().isDirectory());
            assertTrue(paths.logs().toFile().isDirectory());
            assertTrue(paths.customTasks().toFile().isDirectory());
            assertTrue(paths.cache().toFile().isDirectory());
            assertTrue(paths.watcher().toFile().isDirectory());
            assertThrows(IllegalStateException.class, () -> WorkspaceSession.open(paths));
        }
    }

    @Test
    void initializesACompanionWorkspaceWithoutTakingTheApplicationLock() {
        WorkspacePaths paths = new WorkspacePaths(tempDir.resolve("watcher-only"), RuntimeChannel.STABLE);

        WorkspaceSession.initializeLayout(paths);

        assertTrue(paths.marker().toFile().isFile());
        try (WorkspaceSession ignored = WorkspaceSession.open(paths)) {
            assertTrue(paths.applicationLock().toFile().isFile());
        }
    }

    @Test
    void derivesDefaultPortsAndIdentitiesFromTheWorkspace() {
        WorkspacePaths first = new WorkspacePaths(tempDir.resolve("bot-1"), RuntimeChannel.STABLE);
        WorkspacePaths second = new WorkspacePaths(tempDir.resolve("bot-2"), RuntimeChannel.STABLE);

        assertTrue(first.defaultLocalPort() >= 20_000 && first.defaultLocalPort() < 60_000);
        assertTrue(second.defaultLocalPort() >= 20_000 && second.defaultLocalPort() < 60_000);
        assertNotEquals(first.identity(), second.identity());
    }

    @Test
    void derivesIdentityIndependentlyOfJavaStringHashCollisions() {
        WorkspacePaths first = new WorkspacePaths(tempDir.resolve("Aa"), RuntimeChannel.STABLE);
        WorkspacePaths second = new WorkspacePaths(tempDir.resolve("BB"), RuntimeChannel.STABLE);

        assertEquals(first.root().toString().hashCode(), second.root().toString().hashCode());
        assertNotEquals(first.identity(), second.identity());
    }

    @Test
    void rejectsOpeningAWorkspaceWithAnotherChannelIdentity() {
        Path root = tempDir.resolve("shared");
        try (WorkspaceSession ignored = WorkspaceSession.open(
                new WorkspacePaths(root, RuntimeChannel.STABLE))) {
            // Marker is persisted while the first channel owns the workspace.
        }

        assertThrows(IllegalStateException.class, () -> WorkspaceSession.open(
                new WorkspacePaths(root, RuntimeChannel.NIGHTLY)));
    }

    @Test
    void resolvesNamedChannelWorkspaceByDefault() {
        String oldHome = System.getProperty("user.home");
        String oldChannel = System.getProperty(WorkspacePaths.CHANNEL_PROPERTY);
        String oldWorkspace = System.getProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        try {
            System.setProperty("user.home", tempDir.toString());
            System.setProperty(WorkspacePaths.CHANNEL_PROPERTY, "nightly");
            System.clearProperty(WorkspacePaths.WORKSPACE_PROPERTY);

            assertEquals(tempDir.resolve(".frostguard/workspaces/nightly/default").toAbsolutePath(),
                    WorkspacePaths.current().root());
        } finally {
            restore("user.home", oldHome);
            restore(WorkspacePaths.CHANNEL_PROPERTY, oldChannel);
            restore(WorkspacePaths.WORKSPACE_PROPERTY, oldWorkspace);
        }
    }

    private static void restore(String property, String value) {
        if (value == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, value);
        }
    }
}
