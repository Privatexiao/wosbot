package dev.frostguard.engine.helper;

import java.io.IOException;
import java.util.Objects;

import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.nav.CommonGameAreas;
import dev.frostguard.vision.match.OpenCvPatternLocator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotatingMenuPatternEvidenceTest {

    private static final String EVENTS_CALENDAR_FRAME =
            "/navigation/rotating-menu/events-calendar-alliance-mobilization-20260818.png";

    @BeforeAll
    static void loadOpenCv() throws IOException {
        try {
            OpenCvPatternLocator.loadNativeLibrary();
        } catch (UnsatisfiedLinkError ignored) {
            // Another frame test may already have loaded OpenCV in this JVM.
        }
    }

    @Test
    void detectsAllianceMobilizationInEventsHeader() throws IOException {
        ImageSearchResultData result = locate(CommonGameAreas.ROTATING_MENU_HEADER);

        assertTrue(result.isFound(), "Visible Alliance Mobilization tab should be detected: " + result);
        assertTrue(result.getMatchScore() >= 90, "Header tab should meet runtime threshold: " + result);
    }

    @Test
    void rejectsSimilarAllianceMobilizationArtworkOutsideHeader() throws IOException {
        AreaData calendarBody = new AreaData(new PointData(0, 210), new PointData(720, 1280));

        ImageSearchResultData result = locate(calendarBody);

        assertFalse(result.isFound(), "Calendar artwork must not become a clickable tab: " + result);
    }

    private static ImageSearchResultData locate(AreaData area) throws IOException {
        return OpenCvPatternLocator.locatePattern(
                resource(EVENTS_CALENDAR_FRAME),
                TemplatesEnum.ALLIANCE_MOBILIZATION_UNSELECTED_TAB,
                area.topLeft(),
                area.bottomRight(),
                90);
    }

    private static byte[] resource(String path) throws IOException {
        try (var stream = RotatingMenuPatternEvidenceTest.class.getResourceAsStream(path)) {
            return Objects.requireNonNull(stream, "Missing test resource: " + path).readAllBytes();
        }
    }
}
