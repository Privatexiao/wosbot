package dev.frostguard.app;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class FxmlLoadabilityTest {

    private static final List<String> FXML_NAMES = List.of(
            "AllianceChampionshipLayout", "AllianceLayout", "AllianceMobilizationLayout", "AllianceShop",
            "BearTrapLayout", "BeastHuntingLayout", "BulkUpdateDialog", "CharacterLayout",
            "ChiefOrderLayout", "CityEventsExtraLayout", "CityEventsLayout", "CityUpgradesLayout",
            "ConsoleLogLayout", "CustomTasksLayout", "DebuggingLayout", "DummyLayout",
            "EditProfile", "EmuConfigLayout", "EventsLayout", "ExpertsLayout",
            "FishingLayout", "GatherLayout", "GiftcodeLayout", "HospitalLayout", "IntelLayout",
            "LauncherLayout", "NewProfileLayout", "PetsLayout", "PolarTerrorLayout",
            "ProfileManagerLayout", "ResearchLayout", "ScheduleTaskDialog", "ShopLayout",
            "SkipTutorialLayout", "StatisticsLayout", "TaskBuilderLayout", "TaskGanttOverview",
            "TaskManagerLayout", "TelegramLayout", "TrainingLayout"
    );

    @Test
    public void verifyAllFxmlResourcesExist() {
        for (String name : FXML_NAMES) {
            String path = "/layout/" + name + ".fxml";
            try (InputStream is = getClass().getResourceAsStream(path)) {
                assertNotNull(is, "Missing FXML resource: " + path);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read FXML resource: " + path, e);
            }
        }
    }
}
