package dev.frostguard.engine.helper;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.emulator.EmulatorController;
import dev.frostguard.engine.input.TapInteractionService;
import dev.frostguard.engine.nav.CommonGameAreas;
import dev.frostguard.engine.nav.RotatingMenuRoot;
import dev.frostguard.engine.nav.RotatingMenuTarget;
import dev.frostguard.engine.nav.SearchConfigConstants;
import dev.frostguard.vision.logging.ProfileContextLogger;

/** Bounded, destination-verified navigation through the Events and Deals carousels. */
public final class RotatingMenuNavigator {

    static final int NAVIGATION_ATTEMPTS = 2;
    static final int RESET_SWIPES = 3;
    static final int SCAN_SWIPES = 7;

    private static final long ROOT_SETTLE_MS = 2_000L;
    private static final long CAROUSEL_SETTLE_MS = 1_000L;
    private static final long DESTINATION_SETTLE_MS = 1_500L;
    private static final int TAB_THRESHOLD = 90;

    private final Driver driver;
    private final Reporter reporter;

    public RotatingMenuNavigator(
            EmulatorController emu,
            String device,
            AccountDescriptor profile,
            BooleanSupplier recoverHome) {
        this(new RuntimeDriver(emu, device, profile, recoverHome), new RuntimeReporter(profile));
    }

    RotatingMenuNavigator(Driver driver, Reporter reporter) {
        this.driver = driver;
        this.reporter = reporter;
    }

    public boolean navigateTo(RotatingMenuTarget target) {
        for (int attempt = 1; attempt <= NAVIGATION_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                reporter.info("Recovering through Home before retrying " + target);
                if (!driver.recoverHome()) {
                    reporter.warn("Could not recover through Home for " + target);
                    return false;
                }
            }

            reporter.info("Opening " + target.root() + " for " + target
                    + " (attempt " + attempt + "/" + NAVIGATION_ATTEMPTS + ")");
            if (!openRoot(target.root())) {
                reporter.warn(target.root() + " entry was not found for " + target);
                continue;
            }

            TemplatesEnum alreadyOpenEvidence = locateDestinationEvidence(target);
            if (alreadyOpenEvidence != null) {
                reporter.info("Reached " + target + "; verified by " + alreadyOpenEvidence);
                return true;
            }

            LocatedTemplate tab = locateTab(target);
            if (tab == null) {
                tab = resetAndScan(target);
            }
            if (tab == null) {
                reporter.warn("Header target was not found: root=" + target.root()
                        + " target=" + target + " resetSwipes=" + RESET_SWIPES
                        + " scanSwipes=" + SCAN_SWIPES);
                continue;
            }

            reporter.debug("Tapping " + target + " candidate " + tab.template()
                    + " at " + tab.result().getPoint() + " score="
                    + String.format("%.1f", tab.result().getMatchScore()) + "%");
            driver.tap(tab.result());
            driver.waitFor(DESTINATION_SETTLE_MS);

            TemplatesEnum evidence = locateDestinationEvidence(target);
            if (evidence != null) {
                reporter.info("Reached " + target + "; verified by " + evidence);
                return true;
            }

            reporter.warn("Tapped " + target + " candidate " + tab.template()
                    + " but no destination evidence appeared");
        }

        reporter.warn("Navigation failed after bounded retries: root=" + target.root()
                + " target=" + target + " attempts=" + NAVIGATION_ATTEMPTS);
        return false;
    }

    private boolean openRoot(RotatingMenuRoot root) {
        ImageSearchResultData button = driver.locateRoot(root.buttonTemplate());
        if (!found(button)) {
            return false;
        }
        driver.tap(button);
        driver.waitFor(ROOT_SETTLE_MS);
        return true;
    }

    private LocatedTemplate resetAndScan(RotatingMenuTarget target) {
        LocatedTemplate tab = null;
        for (int swipe = 1; swipe <= RESET_SWIPES && tab == null; swipe++) {
            driver.swipe(CommonGameAreas.ROTATING_MENU_RESET_FROM, CommonGameAreas.ROTATING_MENU_RESET_TO);
            driver.waitFor(CAROUSEL_SETTLE_MS);
            tab = locateTab(target);
        }
        for (int swipe = 1; swipe <= SCAN_SWIPES && tab == null; swipe++) {
            driver.swipe(CommonGameAreas.ROTATING_MENU_SCAN_FROM, CommonGameAreas.ROTATING_MENU_SCAN_TO);
            driver.waitFor(CAROUSEL_SETTLE_MS);
            tab = locateTab(target);
        }
        return tab;
    }

    private LocatedTemplate locateTab(RotatingMenuTarget target) {
        for (TemplatesEnum template : target.tabTemplates()) {
            ImageSearchResultData result = driver.locateHeader(template);
            if (found(result)) {
                return new LocatedTemplate(template, result);
            }
        }
        return null;
    }

    private TemplatesEnum locateDestinationEvidence(RotatingMenuTarget target) {
        for (TemplatesEnum template : target.selectedHeaderEvidence()) {
            if (found(driver.locateHeader(template))) {
                return template;
            }
        }
        for (TemplatesEnum template : target.screenEvidence()) {
            if (found(driver.locateScreen(template))) {
                return template;
            }
        }
        return null;
    }

    private static boolean found(ImageSearchResultData result) {
        return result != null && result.isFound();
    }

    private record LocatedTemplate(TemplatesEnum template, ImageSearchResultData result) {}

    interface Driver {
        ImageSearchResultData locateRoot(TemplatesEnum template);

        ImageSearchResultData locateHeader(TemplatesEnum template);

        ImageSearchResultData locateScreen(TemplatesEnum template);

        void tap(ImageSearchResultData result);

        void swipe(PointData from, PointData to);

        void waitFor(long milliseconds);

        boolean recoverHome();
    }

    interface Reporter {
        void info(String message);

        void warn(String message);

        void debug(String message);
    }

    private static final class RuntimeDriver implements Driver {
        private final EmulatorController emu;
        private final String device;
        private final TapInteractionService taps;
        private final TemplateSearchHelper searcher;
        private final BooleanSupplier recoverHome;

        private RuntimeDriver(
                EmulatorController emu,
                String device,
                AccountDescriptor profile,
                BooleanSupplier recoverHome) {
            this.emu = emu;
            this.device = device;
            this.taps = TapInteractionService.forController(emu, device);
            this.searcher = new TemplateSearchHelper(emu, device, profile);
            this.recoverHome = recoverHome;
        }

        @Override
        public ImageSearchResultData locateRoot(TemplatesEnum template) {
            return searcher.locatePattern(template, SearchConfigConstants.SINGLE_WITH_RETRIES);
        }

        @Override
        public ImageSearchResultData locateHeader(TemplatesEnum template) {
            return searcher.locatePattern(template,
                    TemplateSearchHelper.SearchConfig.builder()
                            .withMaxAttempts(1)
                            .withThreshold(TAB_THRESHOLD)
                            .withArea(CommonGameAreas.ROTATING_MENU_HEADER)
                            .build());
        }

        @Override
        public ImageSearchResultData locateScreen(TemplatesEnum template) {
            return searcher.locatePattern(template, SearchConfigConstants.DEFAULT_SINGLE);
        }

        @Override
        public void tap(ImageSearchResultData result) {
            taps.tapInside(result);
        }

        @Override
        public void swipe(PointData from, PointData to) {
            emu.swipeScreen(device, from, to);
        }

        @Override
        public void waitFor(long milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Rotating-menu navigation interrupted");
            }
        }

        @Override
        public boolean recoverHome() {
            return recoverHome.getAsBoolean();
        }
    }

    private static final class RuntimeReporter implements Reporter {
        private final ProfileContextLogger log;

        private RuntimeReporter(AccountDescriptor profile) {
            this.log = new ProfileContextLogger(RotatingMenuNavigator.class, profile);
        }

        @Override
        public void info(String message) {
            log.info(message);
        }

        @Override
        public void warn(String message) {
            log.warn(message);
        }

        @Override
        public void debug(String message) {
            log.debug(message);
        }
    }
}
