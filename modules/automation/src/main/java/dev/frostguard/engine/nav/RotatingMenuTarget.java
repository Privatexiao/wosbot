package dev.frostguard.engine.nav;

import java.util.List;

import dev.frostguard.api.configs.TemplatesEnum;

/**
 * Supported destinations in the rotating Events and Deals header menus.
 *
 * <p>Tab templates establish where to tap. Header or screen evidence must then prove that the
 * intended destination actually opened.</p>
 */
public enum RotatingMenuTarget {
    HERO_MISSION(
            RotatingMenuRoot.EVENTS,
            List.of(TemplatesEnum.HERO_MISSION_EVENT_TAB),
            List.of(),
            List.of(
                    TemplatesEnum.HERO_MISSION_EVENT_TRACE_BUTTON,
                    TemplatesEnum.HERO_MISSION_EVENT_CAPTURE_BUTTON,
                    TemplatesEnum.HERO_MISSION_EVENT_CHEST)),
    MERCENARY(
            RotatingMenuRoot.EVENTS,
            List.of(TemplatesEnum.MERCENARY_EVENT_TAB),
            List.of(),
            List.of(
                    TemplatesEnum.MERCENARY_SCOUT_BUTTON,
                    TemplatesEnum.MERCENARY_CHALLENGE_BUTTON,
                    TemplatesEnum.MERCENARY_DIFFICULTY_CHALLENGE,
                    TemplatesEnum.MERCENARY_EPIC_INITIATION_SELECTED,
                    TemplatesEnum.MERCENARY_EPIC_INITIATION_UNSELECTED,
                    TemplatesEnum.MERCENARY_CHAMPIONS_INITIATION_SELECTED,
                    TemplatesEnum.MERCENARY_CHAMPIONS_INITIATION_UNSELECTED,
                    TemplatesEnum.MERCENARY_LEGENDS_INITIATION_SELECTED,
                    TemplatesEnum.MERCENARY_LEGENDS_INITIATION_UNSELECTED)),
    ALLIANCE_CHAMPIONSHIP(
            RotatingMenuRoot.EVENTS,
            List.of(TemplatesEnum.ALLIANCE_CHAMPIONSHIP_TAB),
            List.of(),
            List.of(
                    TemplatesEnum.ALLIANCE_CHAMPIONSHIP_TROOPS_BUTTON,
                    TemplatesEnum.ALLIANCE_CHAMPIONSHIP_REGISTER_BUTTON,
                    TemplatesEnum.ALLIANCE_CHAMPIONSHIP_SWITCH_LINE_BUTTON,
                    TemplatesEnum.ALLIANCE_CHAMPIONSHIP_UPDATE_TROOPS_BUTTON,
                    TemplatesEnum.ALLIANCE_CHAMPIONSHIP_DISPATCH_TROOPS_BUTTON)),
    ALLIANCE_MOBILIZATION(
            RotatingMenuRoot.EVENTS,
            List.of(
                    TemplatesEnum.ALLIANCE_MOBILIZATION_TAB,
                    TemplatesEnum.ALLIANCE_MOBILIZATION_UNSELECTED_TAB),
            List.of(TemplatesEnum.ALLIANCE_MOBILIZATION_TAB),
            List.of(
                    TemplatesEnum.AM_COMPLETED,
                    TemplatesEnum.AM_PLUS_1_FREE_MISSION,
                    TemplatesEnum.AM_ALLIANCE_MONUMENTS)),
    TUNDRA_TRUCK(
            RotatingMenuRoot.EVENTS,
            List.of(TemplatesEnum.TUNDRA_TRUCK_TAB),
            List.of(),
            List.of(
                    TemplatesEnum.TUNDRA_TRUCK_ARRIVED,
                    TemplatesEnum.TUNDRA_TRUCK_REFRESH,
                    TemplatesEnum.TUNDRA_TRUCK_REFRESH_GEMS,
                    TemplatesEnum.TUNDRA_TRUCK_ESCORT,
                    TemplatesEnum.TUNDRA_TRUCK_DEPARTED,
                    TemplatesEnum.TUNDRA_TRUCK_ENDED,
                    TemplatesEnum.TUNDRA_TRUCK_TIPS_POPUP)),
    JOURNEY_OF_LIGHT(
            RotatingMenuRoot.DEALS,
            List.of(
                    TemplatesEnum.JOURNEY_OF_LIGHT_TAB,
                    TemplatesEnum.JOURNEY_OF_LIGHT_UNSELECTED_TAB),
            List.of(TemplatesEnum.JOURNEY_OF_LIGHT_TAB),
            List.of(
                    TemplatesEnum.JOURNEY_OF_LIGHT_FREE_WATCHES,
                    TemplatesEnum.JOURNEY_OF_LIGHT_CLAIM_WATCHES)),
    BANK(
            RotatingMenuRoot.DEALS,
            List.of(TemplatesEnum.EVENTS_DEALS_BANK),
            List.of(),
            List.of(
                    TemplatesEnum.EVENTS_DEALS_BANK_WITHDRAW,
                    TemplatesEnum.EVENTS_DEALS_BANK_INDEPOSIT,
                    TemplatesEnum.EVENTS_DEALS_BANK_DEPOSIT));

    private final RotatingMenuRoot root;
    private final List<TemplatesEnum> tabTemplates;
    private final List<TemplatesEnum> selectedHeaderEvidence;
    private final List<TemplatesEnum> screenEvidence;

    RotatingMenuTarget(
            RotatingMenuRoot root,
            List<TemplatesEnum> tabTemplates,
            List<TemplatesEnum> selectedHeaderEvidence,
            List<TemplatesEnum> screenEvidence) {
        this.root = root;
        this.tabTemplates = tabTemplates;
        this.selectedHeaderEvidence = selectedHeaderEvidence;
        this.screenEvidence = screenEvidence;
    }

    public RotatingMenuRoot root() {
        return root;
    }

    public List<TemplatesEnum> tabTemplates() {
        return tabTemplates;
    }

    public List<TemplatesEnum> selectedHeaderEvidence() {
        return selectedHeaderEvidence;
    }

    public List<TemplatesEnum> screenEvidence() {
        return screenEvidence;
    }
}
