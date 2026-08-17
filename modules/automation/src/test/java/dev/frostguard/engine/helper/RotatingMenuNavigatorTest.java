package dev.frostguard.engine.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.nav.RotatingMenuTarget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotatingMenuNavigatorTest {

    @Test
    void scansFromResetDirectionAndVerifiesDestinationBeforeSuccess() {
        FakeDriver driver = new FakeDriver();
        driver.headerMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK
                && driver.swipes >= RotatingMenuNavigator.RESET_SWIPES + 1;
        driver.screenMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK_INDEPOSIT
                && driver.targetTaps == 1;

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.BANK);

        assertTrue(reached);
        assertEquals(1, driver.rootTaps);
        assertEquals(1, driver.targetTaps);
        assertEquals(RotatingMenuNavigator.RESET_SWIPES + 1, driver.swipes);
        assertEquals(0, driver.recoveries);
    }

    @Test
    void retriesThroughHomeWhenFirstCandidateOpensWrongScreen() {
        FakeDriver driver = new FakeDriver();
        driver.headerMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK;
        driver.screenMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK_DEPOSIT
                && driver.targetTaps >= 2;

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.BANK);

        assertTrue(reached);
        assertEquals(2, driver.rootTaps);
        assertEquals(2, driver.targetTaps);
        assertEquals(1, driver.recoveries);
    }

    @Test
    void rejectsCandidateWhenDestinationEvidenceNeverAppears() {
        FakeDriver driver = new FakeDriver();
        driver.headerMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK;

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.BANK);

        assertFalse(reached);
        assertEquals(RotatingMenuNavigator.NAVIGATION_ATTEMPTS, driver.targetTaps);
        assertEquals(1, driver.recoveries);
    }

    @Test
    void boundsMissingTargetAcrossBothNavigationAttempts() {
        FakeDriver driver = new FakeDriver();

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.BANK);

        assertFalse(reached);
        assertEquals(2, driver.rootTaps);
        assertEquals(0, driver.targetTaps);
        assertEquals(1, driver.recoveries);
        assertEquals(RotatingMenuNavigator.NAVIGATION_ATTEMPTS
                * (RotatingMenuNavigator.RESET_SWIPES + RotatingMenuNavigator.SCAN_SWIPES),
                driver.swipes);
    }

    @Test
    void acceptsAlreadySelectedTargetOnlyWithDestinationEvidence() {
        FakeDriver driver = new FakeDriver();
        driver.screenMatches = template -> template == TemplatesEnum.EVENTS_DEALS_BANK_WITHDRAW;

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.BANK);

        assertTrue(reached);
        assertEquals(1, driver.rootTaps);
        assertEquals(0, driver.targetTaps);
        assertEquals(0, driver.swipes);
    }

    @Test
    void supportsSeparateUnselectedAndSelectedHeaderTemplates() {
        FakeDriver driver = new FakeDriver();
        driver.headerMatches = template -> switch (template) {
            case JOURNEY_OF_LIGHT_UNSELECTED_TAB -> driver.targetTaps == 0;
            case JOURNEY_OF_LIGHT_TAB -> driver.targetTaps == 1;
            default -> false;
        };

        boolean reached = navigator(driver).navigateTo(RotatingMenuTarget.JOURNEY_OF_LIGHT);

        assertTrue(reached);
        assertEquals(1, driver.targetTaps);
        assertEquals(0, driver.swipes);
    }

    private static RotatingMenuNavigator navigator(FakeDriver driver) {
        return new RotatingMenuNavigator(driver, new RecordingReporter());
    }

    private static final class FakeDriver implements RotatingMenuNavigator.Driver {
        private Predicate<TemplatesEnum> headerMatches = template -> false;
        private Predicate<TemplatesEnum> screenMatches = template -> false;
        private int rootTaps;
        private int targetTaps;
        private int swipes;
        private int recoveries;

        @Override
        public ImageSearchResultData locateRoot(TemplatesEnum template) {
            return hit(10, 10);
        }

        @Override
        public ImageSearchResultData locateHeader(TemplatesEnum template) {
            return headerMatches.test(template) ? hit(110, 130) : ImageSearchResultData.miss();
        }

        @Override
        public ImageSearchResultData locateScreen(TemplatesEnum template) {
            return screenMatches.test(template) ? hit(360, 640) : ImageSearchResultData.miss();
        }

        @Override
        public void tap(ImageSearchResultData result) {
            if (result.getPoint().getX() == 10) {
                rootTaps++;
            } else {
                targetTaps++;
            }
        }

        @Override
        public void swipe(PointData from, PointData to) {
            swipes++;
        }

        @Override
        public void waitFor(long milliseconds) {
        }

        @Override
        public boolean recoverHome() {
            recoveries++;
            return true;
        }

        private static ImageSearchResultData hit(int x, int y) {
            return ImageSearchResultData.hit(x, y, 98.0, 40, 30);
        }
    }

    private static final class RecordingReporter implements RotatingMenuNavigator.Reporter {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void info(String message) {
            messages.add(message);
        }

        @Override
        public void warn(String message) {
            messages.add(message);
        }

        @Override
        public void debug(String message) {
            messages.add(message);
        }
    }
}
