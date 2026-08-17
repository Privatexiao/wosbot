package dev.frostguard.engine.helper;

import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.configs.TpMessageSeverityEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.emulator.EmulatorController;
import dev.frostguard.engine.error.HomeNotFoundException;
import dev.frostguard.engine.error.ProfileInReconnectStateException;
import dev.frostguard.engine.input.TapInteractionService;
import dev.frostguard.engine.nav.ButtonConstants;
import dev.frostguard.engine.nav.CommonGameAreas;
import dev.frostguard.engine.nav.RotatingMenuTarget;
import dev.frostguard.engine.nav.SearchConfigConstants;
import dev.frostguard.engine.nav.SidebarDestination;
import dev.frostguard.engine.nav.SidebarSection;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.engine.service.LoggingService;
import dev.frostguard.vision.logging.ProfileContextLogger;

// Screen location verification and cross-screen navigation for
// primary game views and auxiliary menus.
public class NavigationHelper {

    private static final AreaData LABYRINTH_LEADERBOARD = area(646, 185, 704, 264);
    private static final AreaData LABYRINTH_CATEGORY = area(90, 190, 320, 450);
    private static final AreaData ALLIANCE_POWER_RANKINGS = area(80, 1035, 290, 1100);
    private static final PointData RANKING_SCROLL_FROM = new PointData(360, 900);
    private static final PointData RANKING_SCROLL_TO = new PointData(360, 620);
    private static final int ALLIANCE_POWER_RANKING_SCROLLS = 23;
    private static final int ALLIANCE_POWER_RANKING_SCROLL_DURATION_MS = 250;
    private static final long ALLIANCE_POWER_ROW_LOAD_MS = 500;

    private final TemplateSearchHelper searcher;
    private final EmulatorController emu;
    private final String device;
    private final TapInteractionService taps;
    private final ProfileContextLogger log;
    private final String accountName;
    private final LoggingService logs;
    private final SidebarNavigator sidebar;
    private final RotatingMenuNavigator rotatingMenus;

    public NavigationHelper(EmulatorController emuManager, String emulatorNumber,
                            AccountDescriptor profile) {
        this.emu = emuManager;
        this.device = emulatorNumber;
        this.taps = TapInteractionService.forController(emuManager, emulatorNumber);
        this.searcher = new TemplateSearchHelper(emuManager, emulatorNumber, profile);
        this.log = new ProfileContextLogger(NavigationHelper.class, profile);
        this.accountName = profile.getName();
        this.logs = LoggingService.obtain();
        this.sidebar = new SidebarNavigator(emuManager, emulatorNumber, profile);
        this.rotatingMenus = new RotatingMenuNavigator(emuManager, emulatorNumber, profile, () -> {
            ensureCorrectScreenLocation(LaunchPoint.HOME);
            return true;
        });
    }

    // ── alliance menu ────────────────────────────────────────────────

    public boolean navigateToAllianceMenu(AllianceMenu menu) {
        taps.tapInside(ButtonConstants.BOTTOM_MENU_ALLIANCE_BUTTON);

        TemplatesEnum tpl;
        if (menu == AllianceMenu.WAR) tpl = TemplatesEnum.ALLIANCE_WAR_BUTTON;
        else if (menu == AllianceMenu.CHESTS) tpl = TemplatesEnum.ALLIANCE_CHEST_BUTTON;
        else if (menu == AllianceMenu.TERRITORY) tpl = TemplatesEnum.ALLIANCE_TERRITORY_BUTTON;
        else if (menu == AllianceMenu.SHOP) tpl = TemplatesEnum.ALLIANCE_SHOP_BUTTON;
        else if (menu == AllianceMenu.TECH) tpl = TemplatesEnum.ALLIANCE_TECH_BUTTON;
        else if (menu == AllianceMenu.HELP) tpl = TemplatesEnum.ALLIANCE_HELP_BUTTON;
        else tpl = TemplatesEnum.ALLIANCE_TRIUMPH_BUTTON;

        ImageSearchResultData hit = searcher.locatePattern(tpl,
                SearchConfigConstants.SINGLE_WITH_RETRIES);
        if (!hit.isFound()) return false;
        taps.tapInside(hit, 1, 1000);
        return true;
    }

    public boolean navigateToLabyrinth() {
        ensureCorrectScreenLocation(LaunchPoint.HOME);
        return navigateToSidebarDestination(SidebarDestination.LAND_OF_HEROES);
    }

    public boolean openSidebarSection(SidebarSection section) {
        return sidebar.openSectionAtTop(section);
    }

    public boolean navigateToSidebarDestination(SidebarDestination destination) {
        ensureCorrectScreenLocation(LaunchPoint.ANY);
        broadcastInfo("Navigating through sidebar to " + destination);
        boolean reached = sidebar.navigateTo(destination);
        if (!reached) {
            broadcastWarn("Sidebar navigation failed: " + destination);
        }
        return reached;
    }

    public boolean closeSidebar() {
        return sidebar.close();
    }

    public void navigateToLabyrinthRanking() {
        if (!navigateToLabyrinth()) {
            throw new IllegalStateException("Land of Heroes entry was not found in the Daily sidebar");
        }
        taps.tapInside(LABYRINTH_LEADERBOARD, 1, 2_500);
        for (int attempt = 1; attempt <= 3; attempt++) {
            taps.tapInside(LABYRINTH_CATEGORY, 1, 1_500);
            if (headerContains("THE LABYRINTH")) {
                return;
            }
        }
        throw new IllegalStateException("The Labyrinth ranking did not open");
    }

    public void navigateToPowerRanking() {
        ensureCorrectScreenLocation(LaunchPoint.HOME);
        taps.tapInside(CommonGameAreas.BOTTOM_MENU_ALLIANCE_BUTTON, 1, 2_000);
        ImageSearchResultData allianceAnchor = searcher.locatePattern(
                TemplatesEnum.ALLIANCE_WAR_BUTTON, SearchConfigConstants.SINGLE_WITH_RETRIES);
        if (!allianceAnchor.isFound()) {
            throw new IllegalStateException("Alliance menu did not open");
        }
        taps.tapInside(ALLIANCE_POWER_RANKINGS, 1, 2_500);
        if (!headerContains("ALLIANCE RANKING")) {
            throw new IllegalStateException("Power Ranking did not open");
        }
        interruptibleWait(5_000);
        for (int index = 0; index < ALLIANCE_POWER_RANKING_SCROLLS; index++) {
            emu.swipeScreen(device, RANKING_SCROLL_FROM, RANKING_SCROLL_TO,
                    ALLIANCE_POWER_RANKING_SCROLL_DURATION_MS);
            interruptibleWait(ALLIANCE_POWER_ROW_LOAD_MS);
        }
        interruptibleWait(5_000);
    }

    private boolean headerContains(String expected) {
        try {
            String header = emu.readText(device, new PointData(70, 0), new PointData(500, 80));
            return header != null && header.toUpperCase(java.util.Locale.ROOT).contains(expected);
        } catch (Exception exception) {
            log.warn("Could not verify screen header: " + exception.getMessage());
            return false;
        }
    }

    private static AreaData area(int x1, int y1, int x2, int y2) {
        return new AreaData(new PointData(x1, y1), new PointData(x2, y2));
    }

    // ── event menu ───────────────────────────────────────────────────

    public boolean navigateToRotatingMenu(RotatingMenuTarget target) {
        return rotatingMenus.navigateTo(target);
    }

    public void clearEventTabSelection() {
        taps.tapInside(CommonGameAreas.ROTATING_MENU_HEADER_BACKGROUND, 5, 300);
        interruptibleWait(300);
    }

    // ── screen location ──────────────────────────────────────────────

    public void ensureCorrectScreenLocation(LaunchPoint target) {
        broadcastDebug("Locating screen - need " + target);
        int budget = 10;
        int pass = 1;
        while (pass <= budget) {
            // detect reconnect
            if (searcher.locatePattern(TemplatesEnum.GAME_HOME_RECONNECT,
                    SearchConfigConstants.DEFAULT_SINGLE).isFound()) {
                throw new ProfileInReconnectStateException(accountName + " in reconnect state");
            }

            boolean atHome = searcher.locatePattern(TemplatesEnum.GAME_HOME_FURNACE,
                    SearchConfigConstants.DEFAULT_SINGLE).isFound();
            boolean atWorld = !atHome && searcher.locatePattern(TemplatesEnum.GAME_HOME_WORLD,
                    SearchConfigConstants.DEFAULT_SINGLE).isFound();

            // check if already at desired location
            if (target == LaunchPoint.ANY && (atHome || atWorld)) return;
            if (target == LaunchPoint.HOME && atHome) return;
            if (target == LaunchPoint.WORLD && atWorld) return;

            // try to navigate to desired location
            if (target == LaunchPoint.HOME && atWorld) {
                ImageSearchResultData w = searcher.locatePattern(TemplatesEnum.GAME_HOME_WORLD,
                        SearchConfigConstants.DEFAULT_SINGLE);
                if (w.isFound() && isStableScreenAnchorFlow(TemplatesEnum.GAME_HOME_WORLD)) {
                    taps.tapInside(w);
                    interruptibleWait(2000);
                    if (searcher.locatePattern(TemplatesEnum.GAME_HOME_FURNACE,
                            SearchConfigConstants.DEFAULT_SINGLE).isFound()) return;
                }
            } else if (target == LaunchPoint.WORLD && atHome) {
                ImageSearchResultData h = searcher.locatePattern(TemplatesEnum.GAME_HOME_FURNACE,
                        SearchConfigConstants.DEFAULT_SINGLE);
                if (h.isFound() && isStableScreenAnchorFlow(TemplatesEnum.GAME_HOME_FURNACE)) {
                    taps.tapInside(h);
                    interruptibleWait(2000);
                    if (searcher.locatePattern(TemplatesEnum.GAME_HOME_WORLD,
                            SearchConfigConstants.DEFAULT_SINGLE).isFound()) return;
                }
            }

            // unknown screen - go back
            if (!atHome && !atWorld) {
                broadcastDebug("Unknown screen - back (" + pass + "/" + budget + ")");
                emu.pressBack(device);
                interruptibleWait(300);
            }
            pass++;
        }
        throw new HomeNotFoundException("Home not found after " + budget + " attempts");
    }

    // Require one immediate re-check before tapping a home/world anchor to reduce transient mis-taps.
    private boolean isStableScreenAnchorFlow(TemplatesEnum anchorTemplate) {
        interruptibleWait(120);
        return searcher.locatePattern(anchorTemplate, SearchConfigConstants.DEFAULT_SINGLE).isFound();
    }

    // ── logging shortcuts ────────────────────────────────────────────

    private void broadcastInfo(String msg) {
        log.info(accountName + " - " + msg);
        logs.emit(TpMessageSeverityEnum.INFO, "NavigationHelper", accountName, msg);
    }

    private void broadcastWarn(String msg) {
        log.warn(accountName + " - " + msg);
        logs.emit(TpMessageSeverityEnum.WARNING, "NavigationHelper", accountName, msg);
    }

    private void broadcastDebug(String msg) {
        log.debug(accountName + " - " + msg);
        logs.emit(TpMessageSeverityEnum.DEBUG, "NavigationHelper", accountName, msg);
    }

    private void interruptibleWait(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private enum ScreenState { HOME, WORLD, RECONNECT, UNKNOWN }
    public enum AllianceMenu { WAR, CHESTS, TERRITORY, SHOP, TECH, HELP, TRIUMPH }
}
