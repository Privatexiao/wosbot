package dev.frostguard.engine.service;

import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.RawImageData;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for capturing and saving exception screenshots and anonymized metadata when UI anomalies occur.
 */
public final class ExceptionScreenshotService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ExceptionScreenshotService() {
        // Utility service
    }

    public static boolean saveExceptionEvidence(RawImageData rawImage, AccountDescriptor profile, String taskName, String reason) {
        if (rawImage == null || rawImage.getFrameBytes() == null || rawImage.getFrameBytes().length == 0) {
            return false;
        }

        try {
            String profileId = (profile != null && profile.getId() != null) ? "profile_" + profile.getId() : "global";
            String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
            String safeTask = (taskName != null) ? taskName.replaceAll("[^a-zA-Z0-9_]", "_") : "unknown_task";
            String fileName = timestamp + "_" + profileId + "_" + safeTask + ".png";

            Path outputDir = Paths.get("logs", "screenshots");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Path imagePath = outputDir.resolve(fileName);
            Files.write(imagePath, rawImage.getFrameBytes());

            String metaFileName = timestamp + "_" + profileId + "_" + safeTask + ".meta.txt";
            Path metaPath = outputDir.resolve(metaFileName);
            String metadata = "Timestamp: " + LocalDateTime.now() + "\nProfileID: " + profileId + "\nTask: " + taskName + "\nReason: " + reason + "\nResolution: " + rawImage.getScanlineWidth() + "x" + rawImage.getScanlineCount();
            Files.writeString(metaPath, metadata);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
