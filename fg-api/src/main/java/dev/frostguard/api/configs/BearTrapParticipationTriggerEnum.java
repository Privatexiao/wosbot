package dev.frostguard.api.configs;

public enum BearTrapParticipationTriggerEnum {
    TIMER_ONLY("仅定时器触发 (推荐)", false),
    TIMER_ICON_FALLBACK("定时器 + 图标保底触发", true);

    private final String displayName;
    private final boolean iconFallbackEnabled;

    BearTrapParticipationTriggerEnum(String displayName, boolean iconFallbackEnabled) {
        this.displayName = displayName;
        this.iconFallbackEnabled = iconFallbackEnabled;
    }

    public boolean isIconFallbackEnabled() {
        return iconFallbackEnabled;
    }

    public static BearTrapParticipationTriggerEnum fromIconFallbackEnabled(boolean enabled) {
        return enabled ? TIMER_ICON_FALLBACK : TIMER_ONLY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
