package dev.frostguard.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record BuildMetadata(boolean pullRequestBuild, String authenticodePublisher) {
    private static final String RESOURCE = "/dev/frostguard/app/frostguard-build.properties";

    public static BuildMetadata current() {
        return Holder.INSTANCE;
    }

    static BuildMetadata read(InputStream input) {
        if (input == null) {
            return new BuildMetadata(true, "");
        }
        Properties properties = new Properties();
        try (input) {
            properties.load(input);
        } catch (IOException exception) {
            return new BuildMetadata(true, "");
        }
        String value = properties.getProperty("pullRequestBuild", "").trim();
        if (!value.equals("true") && !value.equals("false")) {
            return new BuildMetadata(true, "");
        }
        return new BuildMetadata(Boolean.parseBoolean(value),
                properties.getProperty("authenticodePublisher", "").trim());
    }

    private static final class Holder {
        private static final BuildMetadata INSTANCE = read(BuildMetadata.class.getResourceAsStream(RESOURCE));
    }
}
