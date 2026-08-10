package dev.frostguard.api.runtime;

import java.util.Locale;

public enum RuntimeChannel {
    DEVELOPMENT,
    NIGHTLY,
    STABLE;

    public String directoryName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RuntimeChannel from(String value) {
        if (value == null || value.isBlank()) {
            return STABLE;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Unsupported Frostguard channel: " + value);
        }
    }
}
