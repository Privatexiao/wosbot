package dev.frostguard.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.RawImageData;
import dev.frostguard.api.runtime.WorkspacePaths;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExceptionScreenshotServiceTest {

    @TempDir
    Path workspace;

    @Test
    void writesValidPngAndMetadataInsideSelectedWorkspace() throws Exception {
        String previous = System.getProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        System.setProperty(WorkspacePaths.WORKSPACE_PROPERTY, workspace.toString());
        try {
            byte[] pixels = {
                    (byte) 255, 0, 0, 0,
                    0, (byte) 255, 0, 0
            };
            RawImageData frame = RawImageData.capture(pixels, 2, 1, 4);
            AccountDescriptor profile = new AccountDescriptor(42L);

            assertTrue(ExceptionScreenshotService.saveExceptionEvidence(
                    frame, profile, "Intel/Task", "unexpected popup"));

            Path output = workspace.resolve("logs").resolve("screenshots");
            Path imagePath;
            try (var files = Files.list(output)) {
                imagePath = files.filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow();
            }
            BufferedImage image = ImageIO.read(imagePath.toFile());
            assertNotNull(image);
            assertEquals(2, image.getWidth());
            assertEquals(1, image.getHeight());

            try (var files = Files.list(output)) {
                assertTrue(files.anyMatch(path -> path.toString().endsWith(".meta.txt")));
            }
        } finally {
            if (previous == null) {
                System.clearProperty(WorkspacePaths.WORKSPACE_PROPERTY);
            } else {
                System.setProperty(WorkspacePaths.WORKSPACE_PROPERTY, previous);
            }
        }
    }
}
