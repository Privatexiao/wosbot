package dev.frostguard.tasks.city;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.error.TaskPreemptedException;
import dev.frostguard.engine.helper.TemplateSearchHelper.SearchConfig;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.tasks.city.hospital.HealBatchCalculator;
import dev.frostguard.tasks.city.hospital.HospitalHealState;
import dev.frostguard.tasks.city.hospital.HospitalPageEvidencePolicy;
import dev.frostguard.tasks.city.hospital.HospitalPageEvidencePolicy.WoundedCountEvidence;
import dev.frostguard.tasks.city.hospital.HospitalSchedulePolicy;
import dev.frostguard.vision.convert.GameTimeUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import static dev.frostguard.api.configs.ConfigurationKeyEnum.*;
import static dev.frostguard.api.configs.TemplatesEnum.*;

/**
 * Hospital Healing routine for automating troop recovery via Field shortcut or City building.
 * Refactored to support state machine and batched healing calculations.
 */
public class HospitalHealRoutine extends DelayedTask {

    public HospitalHealRoutine(AccountDescriptor profile, TpDailyTaskEnum taskType) {
        super(profile, taskType);
    }

    public enum EntryResult {
        ENTERED,
        NOT_AVAILABLE,
        FAILED
    }

    private HospitalHealState state = HospitalHealState.DISCOVER_ENTRY;
    private int batchedAmountToHeal = -1;
    private boolean useFieldEntry = true;
    private boolean useCityEntry = false;
    
    private int totalWounded = 0;
    private long healTimeSec = 0;
    private long singleTroopTimeSec = 0;
    private long currentEstimatedHelpsSec = -1;
    private PointData lastHealBtnPos = null;
    private HospitalSchedulePolicy.Outcome runOutcome = HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE;
    private Duration remainingHealTime;

    private static final PointData TROOP_1_INPUT_BOX_CENTER = new PointData(590, 390);
    private static final PointData TROOP_1_INPUT_BOX_TL = new PointData(540, 360);
    private static final PointData TROOP_1_INPUT_BOX_BR = new PointData(640, 420);

    private static final PointData HEAL_TIME_TL = new PointData(510, 660);
    private static final PointData HEAL_TIME_BR = new PointData(640, 695);
    private static final boolean CITY_HOSPITAL_ENTRY_SUPPORTED = false;

    private boolean resolveConfigBoolean(ConfigurationKeyEnum key, boolean defaultValue) {
        if (profile == null) return defaultValue;
        Boolean val = profile.getConfig(key, Boolean.class);
        return val != null ? val : defaultValue;
    }

    private int resolveConfigInt(ConfigurationKeyEnum key, int defaultValue) {
        if (profile == null) return defaultValue;
        Integer val = profile.getConfig(key, Integer.class);
        return val != null ? val : defaultValue;
    }

    @Override
    protected void execute() {
        if (!resolveConfigBoolean(HOSPITAL_HEAL_ENABLED_BOOL, false)) {
            logInfo(routineLogHospitalLine("Hospital Heal Routine disabled in configuration; skipping execution."));
            return;
        }

        useFieldEntry = resolveConfigBoolean(HOSPITAL_HEAL_FIELD_ENABLED_BOOL, true);
        boolean cityEntryConfigured = resolveConfigBoolean(HOSPITAL_HEAL_CITY_ENABLED_BOOL, false);
        useCityEntry = CITY_HOSPITAL_ENTRY_SUPPORTED && cityEntryConfigured;
        if (cityEntryConfigured && !CITY_HOSPITAL_ENTRY_SUPPORTED) {
            logWarning(routineLogHospitalLine(
                    "City hospital entry remains disabled until its English real-frame template is verified."));
        }

        if (!useFieldEntry && !useCityEntry) {
            logWarning(routineLogHospitalLine("Both field and city hospital entry options are disabled; exiting."));
            reschedule(HospitalSchedulePolicy.nextRun(LocalDateTime.now(),
                    HospitalSchedulePolicy.Outcome.CONFIGURATION_UNSUPPORTED, null));
            return;
        }

        logInfo(routineLogHospitalLine("Starting Hospital Heal Routine execution flow..."));

        state = HospitalHealState.DISCOVER_ENTRY;
        batchedAmountToHeal = -1;
        totalWounded = 0;
        healTimeSec = 0;
        singleTroopTimeSec = 0;
        currentEstimatedHelpsSec = -1;
        lastHealBtnPos = null;
        runOutcome = HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE;
        remainingHealTime = null;
        int safetyCounter = 0;

        try {
            while (state != HospitalHealState.COMPLETE && state != HospitalHealState.ABORT && safetyCounter < 20) {
                checkPreemption();
                safetyCounter++;
                processState();
            }
        } catch (TaskPreemptedException preempted) {
            throw preempted;
        } catch (RuntimeException failure) {
            reschedule(HospitalSchedulePolicy.nextRun(LocalDateTime.now(),
                    HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE, null));
            throw failure;
        }

        if (safetyCounter >= 20) {
            logWarning(routineLogHospitalLine("State machine aborted due to infinite loop prevention."));
        }

        reschedule(HospitalSchedulePolicy.nextRun(LocalDateTime.now(), runOutcome, remainingHealTime));
        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.ANY);
    }

    private void processState() {
        logInfo(routineLogHospitalLine("Processing state: " + state));
        switch (state) {
            case DISCOVER_ENTRY:
                if (useFieldEntry) {
                    state = HospitalHealState.ENTER_FIELD;
                } else {
                    state = HospitalHealState.ENTER_CITY;
                }
                break;

            case ENTER_FIELD:
                EntryResult fieldResult = tryFieldHospitalEntry();
                if (fieldResult == EntryResult.ENTERED) {
                    state = HospitalHealState.CONFIRM_HEAL_SCREEN;
                } else if (useCityEntry) {
                    state = HospitalHealState.ENTER_CITY;
                } else {
                    logInfo(routineLogHospitalLine("Field entry not available and city entry disabled."));
                    runOutcome = HospitalSchedulePolicy.Outcome.NO_ENTRY;
                    state = HospitalHealState.COMPLETE;
                }
                break;

            case ENTER_CITY:
                EntryResult cityResult = tryCityHospitalEntry();
                if (cityResult == EntryResult.ENTERED) {
                    state = HospitalHealState.CONFIRM_HEAL_SCREEN;
                } else {
                    logInfo(routineLogHospitalLine("City entry not available."));
                    runOutcome = HospitalSchedulePolicy.Outcome.NO_ENTRY;
                    state = HospitalHealState.COMPLETE;
                }
                break;

            case CONFIRM_HEAL_SCREEN:
                logInfo(routineLogHospitalLine("Waiting for hospital popup to fully open..."));
                sleepTask(2500);

                WoundedCountEvidence pageWounded = readWoundedCountEvidence("page identity");
                boolean coloredHealButtonVisible = isColoredHealButtonVisible();
                if (!HospitalPageEvidencePolicy.recognizesHealScreen(
                        pageWounded, coloredHealButtonVisible)) {
                    logWarning(routineLogHospitalLine(
                            "Hospital heal screen was not identified; aborting before fixed-area interaction."));
                    runOutcome = HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE;
                    state = HospitalHealState.ABORT;
                    break;
                }
                if (HospitalPageEvidencePolicy.provesNoWounded(pageWounded)) {
                    logInfo(routineLogHospitalLine(
                            "Hospital wounded count is explicitly zero; exiting before fixed-area interaction."));
                    runOutcome = HospitalSchedulePolicy.Outcome.NO_WOUNDED;
                    state = HospitalHealState.COMPLETE;
                    break;
                }

                logInfo(routineLogHospitalLine("Ensuring all troops are unselected by checking Heal button state..."));
                boolean isZeroedOut = false;
                for (int attempts = 0; attempts < 3; attempts++) {
                    ImageSearchResultData healBtnCheck = templateSearchHelper.locatePattern(
                            HOSPITAL_HEAL_BUTTON,
                            SearchConfig.builder().withThreshold(70).withMaxAttempts(1).build());
                    
                    if (!healBtnCheck.isFound()) {
                        logInfo(routineLogHospitalLine("Heal button is gray (not found). Selections are cleared."));
                        isZeroedOut = true;
                        break;
                    }
                    
                    logInfo(routineLogHospitalLine("Heal button is colored. Tapping Quick Select to toggle..."));
                    tapInside(new PointData(134, 852), new PointData(134, 852), 1, 100);
                    sleepTask(1500); // Wait for the UI to update
                }

                if (!isZeroedOut) {
                    logInfo(routineLogHospitalLine("Failed to clear troop selections. Aborting to be safe."));
                    state = HospitalHealState.ABORT;
                    break;
                }

                // Write 1 to ensure the button is active (blue)
                tapInside(TROOP_1_INPUT_BOX_CENTER, TROOP_1_INPUT_BOX_CENTER, 1, 1500);
                emuManager.clearText(EMULATOR_NUMBER, 6);
                emuManager.writeText(EMULATOR_NUMBER, "1\n");
                sleepTask(1000); // wait for UI to update
                // hide keyboard by clicking empty area inside popup (e.g. title area above the list)
                tapInside(new PointData(360, 320), new PointData(360, 320), 1, 500);


                ImageSearchResultData healBtn = templateSearchHelper.locatePattern(
                        HOSPITAL_HEAL_BUTTON,
                        SearchConfig.builder().withThreshold(70).withMaxAttempts(6).build());
                WoundedCountEvidence postInputWounded = healBtn.isFound()
                        ? WoundedCountEvidence.unknown()
                        : readWoundedCountEvidence("post-input classification");
                switch (HospitalPageEvidencePolicy.classifyAfterInput(healBtn.isFound(), postInputWounded)) {
                    case READY:
                        lastHealBtnPos = healBtn.getPoint();
                        state = HospitalHealState.READ;
                        break;
                    case NO_WOUNDED:
                        logInfo(routineLogHospitalLine("Hospital wounded count explicitly reported zero."));
                        runOutcome = HospitalSchedulePolicy.Outcome.NO_WOUNDED;
                        state = HospitalHealState.COMPLETE;
                        break;
                    case RECOGNITION_FAILURE:
                        logWarning(routineLogHospitalLine(
                                "Heal button was not detected after input and zero wounded was not proven; aborting safely."));
                        runOutcome = HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE;
                        state = HospitalHealState.ABORT;
                        break;
                }
                break;

            case SELECT_TIER:
                state = HospitalHealState.READ;
                break;

            case READ:
                logInfo(routineLogHospitalLine("Reading total wounded and single troop time..."));
                
                // Read total wounded count
                WoundedCountEvidence woundedEvidence = readWoundedCountEvidence("batch calculation");
                if (woundedEvidence.recognized() && woundedEvidence.count() <= Integer.MAX_VALUE) {
                    totalWounded = (int) woundedEvidence.count();
                    logInfo(routineLogHospitalLine("Read total wounded count: " + totalWounded));
                } else {
                    totalWounded = 0;
                    logInfo(routineLogHospitalLine("Failed to read total wounded count accurately. Proceeding with legacy compatible path."));
                }
                
                PointData ocrTl = HEAL_TIME_TL;
                PointData ocrBr = HEAL_TIME_BR;
                if (lastHealBtnPos != null) {
                    ocrTl = new PointData(Math.max(0, lastHealBtnPos.getX() - 120), Math.max(0, lastHealBtnPos.getY() - 60));
                    ocrBr = new PointData(lastHealBtnPos.getX() + 120, lastHealBtnPos.getY() + 60);
                }

                Duration duration = durationHelper.attemptRecognition(
                    ocrTl,
                    ocrBr,
                    3,
                    200L,
                    null,
                    GameTimeUtils::isAcceptedFormat,
                    GameTimeUtils::parseDuration
                );
                
                if (duration == null || duration.getSeconds() <= 0) {
                    logWarning(routineLogHospitalLine("Failed to read heal time for 1 troop."));
                    state = HospitalHealState.ABORT;
                    break;
                }
                singleTroopTimeSec = duration.getSeconds();
                logInfo(routineLogHospitalLine("Read single troop time: " + singleTroopTimeSec + "s"));
                state = HospitalHealState.CALCULATE;
                break;

            case CALCULATE:
                int helpCount = resolveConfigInt(ConfigurationKeyEnum.ALLIANCE_HELP_MAX_COUNT_INT, 15);
                int reductionSec = resolveConfigInt(ConfigurationKeyEnum.ALLIANCE_HELP_TIME_REDUCTION_SEC_INT, 210);
                currentEstimatedHelpsSec = (long) helpCount * reductionSec;
                if (totalWounded > 0) {
                    long estimatedTotalTime;
                    try {
                        estimatedTotalTime = Math.multiplyExact((long) totalWounded, singleTroopTimeSec);
                    } catch (ArithmeticException overflow) {
                        estimatedTotalTime = -1;
                    }
                    batchedAmountToHeal = new HealBatchCalculator(
                            totalWounded, estimatedTotalTime, helpCount, reductionSec).calculateBatchSize();
                } else {
                    batchedAmountToHeal = HealBatchCalculator.calculateLegacyCompatibleBatchSize(
                            singleTroopTimeSec, helpCount, reductionSec);
                }
                if (batchedAmountToHeal <= 0) {
                    logWarning(routineLogHospitalLine(
                            "Alliance-help calibration is unavailable; refusing to start treatment."));
                    runOutcome = HospitalSchedulePolicy.Outcome.CONFIGURATION_UNSUPPORTED;
                    state = HospitalHealState.ABORT;
                    break;
                }

                logInfo(routineLogHospitalLine("Calculated batch size: " + batchedAmountToHeal + " (Target heal time: " + currentEstimatedHelpsSec + "s)"));
                state = HospitalHealState.INPUT;
                break;

            case INPUT:
                logInfo(routineLogHospitalLine("Inputting batch amount: " + batchedAmountToHeal));
                boolean inputVerified = false;
                for (int attempt = 0; attempt < 2; attempt++) {
                    tapInside(TROOP_1_INPUT_BOX_CENTER, TROOP_1_INPUT_BOX_CENTER, 1, 1500);
                    emuManager.clearText(EMULATOR_NUMBER, 6);
                    emuManager.writeText(EMULATOR_NUMBER, String.valueOf(batchedAmountToHeal) + "\n");
                    sleepTask(1000);
                    // hide keyboard by clicking empty area inside popup
                    tapInside(new PointData(360, 320), new PointData(360, 320), 1, 500);
                    
                    // OCR verification of the input box
                    String readBackRaw = null;
                    try {
                        readBackRaw = provider.extractText(null, TROOP_1_INPUT_BOX_TL, TROOP_1_INPUT_BOX_BR);
                    } catch (Exception e) {
                        logWarning(routineLogHospitalLine("Exception while reading back troop input OCR: " + e.getMessage()));
                    }
                    
                    long readBackVal = dev.frostguard.vision.convert.CompactGameNumberParser.parseCompactNumber(readBackRaw);
                    if (readBackVal == batchedAmountToHeal) {
                        logInfo(routineLogHospitalLine("Input amount verified via OCR: " + readBackVal));
                        inputVerified = true;
                        break;
                    } else if (readBackVal < 0 && isColoredHealButtonVisible()) {
                        logWarning(routineLogHospitalLine(
                                "Input OCR was unavailable, but the Heal button became active; continuing on the legacy-compatible path."));
                        inputVerified = true;
                        break;
                    } else {
                        logWarning(routineLogHospitalLine("Input amount OCR mismatch: expected " + batchedAmountToHeal
                                + ", read '" + readBackRaw + "' (" + readBackVal + "). Retrying..."));
                    }
                }
                
                if (inputVerified) {
                    state = HospitalHealState.START;
                } else {
                    logWarning(routineLogHospitalLine("Failed to verify input amount after retries; aborting to avoid improper heal batches."));
                    state = HospitalHealState.ABORT;
                }
                break;

            case START:
                ImageSearchResultData btn = templateSearchHelper.locatePattern(
                        HOSPITAL_HEAL_BUTTON,
                        SearchConfig.builder().withThreshold(70).withMaxAttempts(4).build());
                if (btn.isFound()) {
                    tapInside(btn);
                    sleepTask(1000);
                    if (isColoredHealButtonVisible()) {
                        logWarning(routineLogHospitalLine(
                                "Heal button remained active after tapping; treatment start was not confirmed."));
                        state = HospitalHealState.ABORT;
                    } else {
                        state = HospitalHealState.REQUEST_HELP;
                    }
                } else {
                    logInfo(routineLogHospitalLine("Heal button not found at START."));
                    state = HospitalHealState.ABORT;
                }
                break;

            case REQUEST_HELP:
                ImageSearchResultData helpBtn = templateSearchHelper.locatePattern(
                        ALLIANCE_HELP_BUTTON,
                        SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());
                if (helpBtn.isFound()) {
                    logInfo(routineLogHospitalLine("Requesting alliance help..."));
                    tapInside(helpBtn);
                    sleepTask(1000);
                }
                state = HospitalHealState.MONITOR;
                break;
                
            case MONITOR:
                logInfo(routineLogHospitalLine("Waiting 30 seconds for alliance helps to apply..."));
                sleepTask(30000);
                
                dev.frostguard.api.domain.AreaData healTimeArea = dev.frostguard.engine.nav.CommonGameAreas.HOSPITAL_HEAL_TIME_OCR_AREA;
                PointData monitorTl = healTimeArea.topLeft();
                PointData monitorBr = healTimeArea.bottomRight();
                
                Duration remaining = durationHelper.attemptRecognition(
                    monitorTl,
                    monitorBr,
                    3,
                    500L,
                    null,
                    GameTimeUtils::isAcceptedFormat,
                    GameTimeUtils::parseDuration
                );
                
                if (remaining == null) {
                    PointData fallbackTl = new PointData(100, 800);
                    PointData fallbackBr = new PointData(620, 1200);
                    remaining = durationHelper.attemptRecognition(
                        fallbackTl,
                        fallbackBr,
                        2,
                        500L,
                        null,
                        GameTimeUtils::isAcceptedFormat,
                        GameTimeUtils::parseDuration
                    );
                }
                
                int configuredMaxWait = resolveConfigInt(
                        ConfigurationKeyEnum.HOSPITAL_HEAL_MAX_WAIT_MINUTES_INT, 30);

                if (remaining != null) {
                    long remainingSec = remaining.getSeconds();
                    remainingHealTime = remaining;
                    runOutcome = HospitalSchedulePolicy.Outcome.ACTIVE_HEAL;
                    logInfo(routineLogHospitalLine("Remaining heal time after helps: " + remainingSec + "s"));
                    if (HospitalSchedulePolicy.exceedsWarningThreshold(
                            remaining, configuredMaxWait)) {
                        logWarning(routineLogHospitalLine(
                                "Remaining heal time exceeds the configured warning threshold; leaving the active heal untouched."));
                    }
                    state = HospitalHealState.COMPLETE;
                } else {
                    logWarning(routineLogHospitalLine("Could not read remaining time; scheduling a conservative retry."));
                    runOutcome = HospitalSchedulePolicy.Outcome.RECOGNITION_FAILURE;
                    state = HospitalHealState.ABORT;
                }
                break;
                
            default:
                state = HospitalHealState.ABORT;
                break;
        }
    }

    private EntryResult tryFieldHospitalEntry() {
        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.WORLD);
        ImageSearchResultData fieldIcon = templateSearchHelper.locatePattern(
                HOSPITAL_FIELD_ICON,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (!fieldIcon.isFound()) {
            return EntryResult.NOT_AVAILABLE;
        }
        logInfo(routineLogHospitalLine("Field hospital shortcut detected."));
        tapInside(fieldIcon);
        sleepTask(1200);
        return EntryResult.ENTERED;
    }

    private WoundedCountEvidence readWoundedCountEvidence(String purpose) {
        dev.frostguard.api.domain.AreaData woundedArea =
                dev.frostguard.engine.nav.CommonGameAreas.HOSPITAL_WOUNDED_COUNT_OCR_AREA;
        try {
            String raw = provider.extractText(null, woundedArea.topLeft(), woundedArea.bottomRight());
            return HospitalPageEvidencePolicy.parseWoundedCount(raw);
        } catch (Exception e) {
            logDebug(routineLogHospitalLine(
                    "Hospital wounded-count OCR was unavailable during " + purpose + ": " + e.getMessage()));
            return WoundedCountEvidence.unknown();
        }
    }

    private boolean isColoredHealButtonVisible() {
        return templateSearchHelper.locatePattern(
                HOSPITAL_HEAL_BUTTON,
                SearchConfig.builder().withThreshold(70).withMaxAttempts(1).build()).isFound();
    }

    private EntryResult tryCityHospitalEntry() {
        navigationHelper.ensureCorrectScreenLocation(LaunchPoint.HOME);
        if (!HOSPITAL_CITY_BUILDING.existsAtPath()) {
            logWarning(routineLogHospitalLine(
                    "City hospital template is unavailable; skipping city entry without a blind tap."));
            return EntryResult.FAILED;
        }
        ImageSearchResultData cityBuilding = templateSearchHelper.locatePattern(
                HOSPITAL_CITY_BUILDING,
                SearchConfig.builder().withThreshold(85).withMaxAttempts(2).build());

        if (!cityBuilding.isFound()) {
            return EntryResult.NOT_AVAILABLE;
        }
        logInfo(routineLogHospitalLine("City hospital detected."));
        tapInside(cityBuilding);
        sleepTask(1200);
        return EntryResult.ENTERED;
    }

    private String routineLogHospitalLine(String msg) {
        return "[HospitalHealRoutine] " + msg;
    }
}
