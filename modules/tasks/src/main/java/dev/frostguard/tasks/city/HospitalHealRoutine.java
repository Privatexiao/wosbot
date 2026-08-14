package dev.frostguard.tasks.city;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.helper.TemplateSearchHelper.SearchConfig;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.engine.service.ExceptionScreenshotService;
import static dev.frostguard.api.configs.ConfigurationKeyEnum.*;
import static dev.frostguard.api.configs.TemplatesEnum.*;

/**
 * Hospital Healing routine for automating troop recovery via Field shortcut or City building.
 */
public class HospitalHealRoutine extends DelayedTask {

    public HospitalHealRoutine(AccountDescriptor profile, TpDailyTaskEnum taskType) {
        super(profile, taskType);
    }

    private static final PointData FIELD_HOSPITAL_ICON_POINT = new PointData(650, 850);
    private static final PointData HEAL_BUTTON_POINT = new PointData(500, 1000);
    private static final PointData HELP_BUTTON_POINT = new PointData(600, 1000);

    public enum EntryResult {
        ENTERED,
        NOT_AVAILABLE,
        FAILED
    }

    @Override
    protected void execute() {
        Boolean enabled = profile.getConfig(HOSPITAL_HEAL_ENABLED_BOOL, Boolean.class);
        if (!Boolean.TRUE.equals(enabled)) {
            logInfo(routineLogHospitalLine("Hospital Heal Routine disabled in configuration; skipping execution in 0.1s without screen interaction."));
            return;
        }

        logInfo(routineLogHospitalLine("Starting Hospital Heal Routine execution flow..."));

        Boolean fieldEnabled = profile.getConfig(HOSPITAL_HEAL_FIELD_ENABLED_BOOL, Boolean.class);
        Boolean cityEnabled = profile.getConfig(HOSPITAL_HEAL_CITY_ENABLED_BOOL, Boolean.class);

        if (!Boolean.TRUE.equals(fieldEnabled) && !Boolean.TRUE.equals(cityEnabled)) {
            logWarning(routineLogHospitalLine("Both field and city hospital entry options are disabled; exiting."));
            return;
        }

        EntryResult entry = tryFieldHospitalEntry();
        if (entry != EntryResult.ENTERED && Boolean.TRUE.equals(cityEnabled)) {
            entry = tryCityHospitalEntry();
        }

        if (entry != EntryResult.ENTERED) {
            logInfo(routineLogHospitalLine("No wounded troops or hospital entry icon present. Exiting routine."));
            navigationHelper.ensureCorrectScreenLocation(LaunchPoint.ANY);
            return;
        }

        processHealingScreenFlow();
    }

    private EntryResult tryFieldHospitalEntry() {
        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.WORLD);
        ImageSearchResultData fieldIcon = templateSearchHelper.locatePattern(
                HOSPITAL_FIELD_ICON,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (!fieldIcon.isFound()) {
            return EntryResult.NOT_AVAILABLE;
        }

        logInfo(routineLogHospitalLine("Field hospital shortcut icon detected. Entering field hospital..."));
        tapInside(fieldIcon.getPoint(), fieldIcon.getPoint(), 1, 100);
        sleepTask(1200);
        return EntryResult.ENTERED;
    }

    private EntryResult tryCityHospitalEntry() {
        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.HOME);
        ImageSearchResultData cityBuilding = templateSearchHelper.locatePattern(
                HOSPITAL_CITY_BUILDING,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (!cityBuilding.isFound()) {
            return EntryResult.NOT_AVAILABLE;
        }

        logInfo(routineLogHospitalLine("City hospital building detected. Entering city hospital..."));
        tapInside(cityBuilding.getPoint(), cityBuilding.getPoint(), 1, 100);
        sleepTask(1200);
        return EntryResult.ENTERED;
    }

    private void processHealingScreenFlow() {
        logInfo(routineLogHospitalLine("Processing hospital healing screen..."));
        ImageSearchResultData healBtn = templateSearchHelper.locatePattern(
                HOSPITAL_HEAL_BUTTON,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (healBtn.isFound()) {
            logInfo(routineLogHospitalLine("Heal button found. Tapping Heal..."));
            tapInside(healBtn);
            sleepTask(1000);

            requestAllianceHelpFlow();
        } else {
            logInfo(routineLogHospitalLine("No active heal button found (zero wounded or already healing)."));
        }

        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.ANY);
    }

    private void requestAllianceHelpFlow() {
        ImageSearchResultData helpBtn = templateSearchHelper.locatePattern(
                HOSPITAL_HELP_BUTTON,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (helpBtn.isFound()) {
            logInfo(routineLogHospitalLine("Requesting alliance help for healing..."));
            tapInside(helpBtn);
            sleepTask(500);
        }
    }

    private String routineLogHospitalLine(String msg) {
        return "[HospitalHealRoutine] " + msg;
    }
}
