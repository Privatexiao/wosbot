package dev.frostguard.engine.nav;

import dev.frostguard.api.configs.TemplatesEnum;

/** Home-screen entry point for a rotating top-bar menu. */
public enum RotatingMenuRoot {
    EVENTS(TemplatesEnum.HOME_EVENTS_BUTTON),
    DEALS(TemplatesEnum.HOME_DEALS_BUTTON);

    private final TemplatesEnum buttonTemplate;

    RotatingMenuRoot(TemplatesEnum buttonTemplate) {
        this.buttonTemplate = buttonTemplate;
    }

    public TemplatesEnum buttonTemplate() {
        return buttonTemplate;
    }
}
