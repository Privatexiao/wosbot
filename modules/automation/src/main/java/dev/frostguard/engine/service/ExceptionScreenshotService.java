package dev.frostguard.engine.service;

import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.RawImageData;
import dev.frostguard.api.runtime.WorkspacePaths;
import dev.frostguard.vision.convert.ImageConverter;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service for capturing and saving exception screenshots and anonymized metadata when UI anomalies occur.
 */
public final class ExceptionScreenshotService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final Duration RETENTION = Duration.ofDays(7);
    private static final int MAX_FILES = 100;
    private static final long MAX_TOTAL_BYTES = 50L * 1024 * 1024;
    private static final int PIXELATION_BLOCK_SIZE = 16;

    private ExceptionScreenshotService() {
        // Utility service
    }

    public static boolean saveExceptionEvidence(RawImageData rawImage, AccountDescriptor profile, String taskName, String reason) {
        if (rawImage == null || rawImage.getFrameBytes() == null || rawImage.getFrameBytes().length == 0) {
            return false;
        }

        try {
            String timestamp = LocalDateTime.now().format(FILE_TIME_FORMATTER);
            String safeTask = (taskName != null) ? taskName.replaceAll("[^a-zA-Z0-9_]", "_") : "unknown_task";
            safeTask = safeTask.substring(0, Math.min(40, safeTask.length()));
            String evidenceId = "exception_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8);
            String fileName = evidenceId + "_" + safeTask + ".png";

            Path outputDir = WorkspacePaths.current().logs().resolve("screenshots");
            Files.createDirectories(outputDir);
            cleanup(outputDir, Instant.now());

            Path imagePath = outputDir.resolve(fileName);
            String metaFileName = evidenceId + "_" + safeTask + ".meta.txt";
            Path metaPath = outputDir.resolve(metaFileName);
            Path tempImage = Files.createTempFile(outputDir, "exception_tmp_", ".png.tmp");
            Path tempMeta = Files.createTempFile(outputDir, "exception_tmp_", ".meta.tmp");
            try {
                BufferedImage redacted = pixelate(ImageConverter.toBufferedImage(rawImage));
                if (!ImageIO.write(redacted, "png", tempImage.toFile())) return false;
                String metadata = "Timestamp: " + LocalDateTime.now()
                        + "\nProfile: " + (profile == null ? "global" : "redacted")
                        + "\nTask: " + safeTask
                        + "\nReasonDigest: " + digest(reason)
                        + "\nPrivacy: full-frame-pixelated"
                        + "\nResolution: " + rawImage.getScanlineWidth() + "x" + rawImage.getScanlineCount();
                Files.writeString(tempMeta, metadata, StandardCharsets.UTF_8);
                Files.move(tempMeta, metaPath, StandardCopyOption.REPLACE_EXISTING);
                try {
                    Files.move(tempImage, imagePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception imageMoveFailure) {
                    Files.deleteIfExists(metaPath);
                    throw imageMoveFailure;
                }
            } finally {
                Files.deleteIfExists(tempImage);
                Files.deleteIfExists(tempMeta);
            }
            cleanup(outputDir, Instant.now());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static BufferedImage pixelate(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            for (int y = 0; y < source.getHeight(); y += PIXELATION_BLOCK_SIZE) {
                for (int x = 0; x < source.getWidth(); x += PIXELATION_BLOCK_SIZE) {
                    int width = Math.min(PIXELATION_BLOCK_SIZE, source.getWidth() - x);
                    int height = Math.min(PIXELATION_BLOCK_SIZE, source.getHeight() - y);
                    graphics.setColor(new Color(source.getRGB(x, y), true));
                    graphics.fillRect(x, y, width, height);
                }
            }
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private static String digest(String value) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(hash, 0, 8);
    }

    static void cleanup(Path outputDir, Instant now) throws Exception {
        if (!Files.isDirectory(outputDir)) return;
        List<Path> files;
        try (var stream = Files.list(outputDir)) {
            files = stream.filter(ExceptionScreenshotService::isManagedEvidence)
                    .sorted(Comparator.comparingLong(ExceptionScreenshotService::lastModified))
                    .toList();
        }
        Instant cutoff = now.minus(RETENTION);
        for (Path file : files) {
            if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) Files.deleteIfExists(file);
        }
        try (var stream = Files.list(outputDir)) {
            files = stream.filter(ExceptionScreenshotService::isManagedEvidence)
                    .sorted(Comparator.comparingLong(ExceptionScreenshotService::lastModified))
                    .toList();
        }
        long totalBytes = 0;
        for (Path file : files) totalBytes += Files.size(file);
        int index = 0;
        while ((files.size() - index > MAX_FILES || totalBytes > MAX_TOTAL_BYTES) && index < files.size()) {
            Path file = files.get(index++);
            totalBytes -= Files.size(file);
            Files.deleteIfExists(file);
        }
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return Long.MIN_VALUE;
        }
    }

    private static boolean isManagedEvidence(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().startsWith("exception_");
    }
}
