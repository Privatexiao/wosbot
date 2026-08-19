package dev.frostguard.tasks.city.hospital;

import dev.frostguard.vision.convert.CompactGameNumberParser;

public final class HospitalPageEvidencePolicy {

    public enum PostInputResult {
        READY,
        NO_WOUNDED,
        RECOGNITION_FAILURE
    }

    public record WoundedCountEvidence(boolean recognized, long count, long capacity) {
        public static WoundedCountEvidence unknown() {
            return new WoundedCountEvidence(false, -1, -1);
        }
    }

    private HospitalPageEvidencePolicy() {
    }

    public static WoundedCountEvidence parseWoundedCount(String raw) {
        if (raw == null || raw.isBlank() || !raw.contains("/")) {
            return WoundedCountEvidence.unknown();
        }
        String[] parts = raw.split("/", 2);
        long count = CompactGameNumberParser.parseCompactNumber(parts[0]);
        long capacity = CompactGameNumberParser.parseCompactNumber(parts[1]);
        if (count < 0 || capacity <= 0 || count > capacity) {
            return WoundedCountEvidence.unknown();
        }
        return new WoundedCountEvidence(true, count, capacity);
    }

    public static boolean recognizesHealScreen(
            WoundedCountEvidence woundedCount, boolean coloredHealButtonVisible) {
        return coloredHealButtonVisible || woundedCount != null && woundedCount.recognized();
    }

    public static boolean provesNoWounded(WoundedCountEvidence woundedCount) {
        return woundedCount != null && woundedCount.recognized() && woundedCount.count() == 0;
    }

    public static PostInputResult classifyAfterInput(
            boolean coloredHealButtonVisible, WoundedCountEvidence woundedCount) {
        if (coloredHealButtonVisible) {
            return PostInputResult.READY;
        }
        if (provesNoWounded(woundedCount)) {
            return PostInputResult.NO_WOUNDED;
        }
        return PostInputResult.RECOGNITION_FAILURE;
    }
}
