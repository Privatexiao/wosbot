package dev.frostguard.tasks.alliance;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.helper.NavigationHelper;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.engine.schedule.TaskQueue;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import dev.frostguard.api.domain.RawImageData;
import dev.frostguard.vision.color.PixelStats;
import dev.frostguard.vision.ocr.TesseractOcrProvider;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class AllianceAutojoinRoutine extends DelayedTask {

private static final AreaData RALLY_SECTION_TAB_VALUE = new AreaData(
			new PointData(81, 114),
			new PointData(195, 152));

private static final AreaData AUTOJOIN_SETTINGS_BUTTON_VALUE = new AreaData(
			new PointData(260, 1200),
			new PointData(450, 1240));

private static final PointData USE_ALL_TROOPS_BUTTON_VALUE = new PointData(98, 376);

private static final PointData SPECIFIC_FORMATION_BUTTON_VALUE = new PointData(98, 442);

private static final PointData QUEUE_COUNTER_SWIPE_START_VALUE = new PointData(430, 600);

private static final PointData QUEUE_COUNTER_SWIPE_END_VALUE = new PointData(40, 600);

private static final AreaData QUEUE_INCREMENT_BUTTON_VALUE = new AreaData(
			new PointData(460, 590),
			new PointData(497, 610));

private static final AreaData ENABLE_AUTOJOIN_BUTTON_VALUE = new AreaData(
			new PointData(380, 1070),
			new PointData(640, 1120));

private static final int MIN_QUEUE_COUNT_FLOOR = 1;

private static final int MAX_QUEUE_COUNT_LIMIT = 6;

private static final int DEFAULT_QUEUE_COUNT_VALUE = 3;

private static final int SCHEDULE_HOURS_VALUE = 7;

private static final int SCHEDULE_MINUTES_VALUE = 50;

private static final int LOOKAHEAD_RETRY_MINUTES_VALUE = 5;

private boolean useAllTroops;

private int queueCount;

public AllianceAutojoinRoutine(AccountDescriptor profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

@Override
	protected void execute() {

		hydrateConfiguration();

		Boolean autojoinEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_BOOL, Boolean.class);
		if (!Boolean.TRUE.equals(autojoinEnabled)) {
			logInfo(routineLogAllianceAutojoinLine("Alliance autojoin task turned OFF in GUI profile configuration. Skipping autojoin execution."));
			return;
		}

		if (deferForUpcomingAutoJoinReset()) {
			return;
		}

		if (!openUpAllianceWarMenu()) {
			manageTaskFailure("Failed to open Alliance War menu");
			return;
		}

		if (!openUpAutoJoinSettings()) {
			manageTaskFailure("Failed to open auto-join ocrPreset");
			return;
		}

		scanAndConfigureTargetsFlow(profile);

		configureTroopSelectionFlow();
		setAutoJoinQueuesFlow(queueCount);
		enableAutoJoinFlow();

		queueNextRun();
	}

private boolean deferForUpcomingAutoJoinReset() {
		TaskQueue queue = scheduleService.getCoordinator().getQueue(profile.getId());
		if (queue == null) return false;
		List<TpDailyTaskEnum> queuedTasks = queue.getNextQueuedTaskTypes(
				AutojoinActivationPolicy.LOOKAHEAD_TASK_COUNT);
		Optional<TpDailyTaskEnum> resetTask = AutojoinActivationPolicy.findUpcomingResetTask(queuedTasks);
		if (resetTask.isEmpty()) return false;
		reschedule(LocalDateTime.now().plusMinutes(LOOKAHEAD_RETRY_MINUTES_VALUE));
		logInfo(routineLogAllianceAutojoinLine("Deferring activation because "
				+ resetTask.get().getName() + " will restore auto-join shortly"));
		return true;
	}

@Override
	protected LaunchPoint getRequiredStartLocation() {
		return LaunchPoint.ANY;
	}

private String routineLogAllianceAutojoinLine(String note) {
        return "AllianceAutojoinRoutine | " + note;
    }

private void configureTroopSelectionFlow() {
		if (useAllTroops) {
			logInfo(routineLogAllianceAutojoinLine("Selecting 'Use all troops' option"));
			tapNear(USE_ALL_TROOPS_BUTTON_VALUE);
		} else {
			logInfo(routineLogAllianceAutojoinLine("Selecting 'Specific formation' option"));
			tapNear(SPECIFIC_FORMATION_BUTTON_VALUE);
		}
		sleepTask(700);

	}

private boolean openUpAutoJoinSettings() {
		logDebug(routineLogAllianceAutojoinLine("Entering rally section"));
		tapInside(RALLY_SECTION_TAB_VALUE.topLeft(), RALLY_SECTION_TAB_VALUE.bottomRight());
		sleepTask(500);


		logDebug(routineLogAllianceAutojoinLine("Entering auto-join ocrPreset popup"));
		tapInside(AUTOJOIN_SETTINGS_BUTTON_VALUE.topLeft(), AUTOJOIN_SETTINGS_BUTTON_VALUE.bottomRight());
		sleepTask(1500);


		logDebug(routineLogAllianceAutojoinLine("Auto-join ocrPreset popup should be open"));
		return true;
	}

private void setAutoJoinQueuesFlow(int count) {
		logInfo(routineLogAllianceAutojoinLine("Applying auto-join queue count to " + count));


		logDebug(routineLogAllianceAutojoinLine("Resetting queue counter to zero"));
		swipe(QUEUE_COUNTER_SWIPE_START_VALUE, QUEUE_COUNTER_SWIPE_END_VALUE);
		sleepTask(300);


		if (count > 1) {
			logDebug(routineLogAllianceAutojoinLine("Incrementing queue counter " + (count - 1) + " times"));
			tapInside(
					QUEUE_INCREMENT_BUTTON_VALUE.topLeft(),
					QUEUE_INCREMENT_BUTTON_VALUE.bottomRight(),
					(count - 1),
					400

			);
			sleepTask(300);

		}

		logDebug(routineLogAllianceAutojoinLine("Queue count set to " + count));
	}

	private void scanAndConfigureTargetsFlow(AccountDescriptor profile) {
		logInfo(routineLogAllianceAutojoinLine("Starting to scan Auto-Join targets..."));

		boolean polarTerrorEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_POLAR_TERROR_BOOL, Boolean.class);
		boolean ginasRevengeEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_GINAS_REVENGE_BOOL, Boolean.class);
		boolean mercenaryPrestigeEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_MERCENARY_PRESTIGE_BOOL, Boolean.class);
		boolean skipMaxedEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_SKIP_MAXED_BOOL, Boolean.class);

		for (int swipe = 0; swipe < 2; swipe++) {
			boolean foundAny = false;
			int LIST_START_Y = 500;
			int SLICE_HEIGHT = 135;
			int SLICE_COUNT = 4;

			for (int i = 0; i < SLICE_COUNT; i++) {
				int startY = LIST_START_Y + (i * SLICE_HEIGHT);
				int endY = startY + SLICE_HEIGHT;
				PointData tl = new PointData(40, startY);
				PointData br = new PointData(700, endY);
				String text = readStringValue(tl, br, dev.frostguard.engine.nav.CommonOCRSettings.AUTOJOIN_REWARD_SETTINGS);

				if (text != null && !text.trim().isEmpty()) {
					boolean processed = processOcrSlice(text, startY, endY, polarTerrorEnabled, ginasRevengeEnabled, mercenaryPrestigeEnabled, skipMaxedEnabled);
					if (processed) foundAny = true;
				}
			}

			if (!foundAny && swipe > 0) break;

			swipe(new PointData(350, 950), new PointData(350, 500));
			sleepTask(1500);
		}
	}

	private boolean processOcrSlice(String text, int startY, int endY, boolean polarTerrorEnabled, boolean ginasRevengeEnabled, boolean mercenaryPrestigeEnabled, boolean skipMaxedEnabled) {
		String lowerText = text.toLowerCase();
		boolean isTarget = false;
		boolean isConfiguredEnabled = false;
		String targetName = "";

		if (lowerText.contains("polar") || lowerText.contains("terror") || lowerText.contains("极地恶魔")) {
			isTarget = true;
			isConfiguredEnabled = polarTerrorEnabled;
			targetName = "Polar Terror";
		} else if (lowerText.contains("gina") || lowerText.contains("revenge") || lowerText.contains("吉娜")) {
			isTarget = true;
			isConfiguredEnabled = ginasRevengeEnabled;
			targetName = "Gina's Revenge";
		} else if (lowerText.contains("mercenary") || lowerText.contains("prestige") || lowerText.contains("佣兵")) {
			isTarget = true;
			isConfiguredEnabled = mercenaryPrestigeEnabled;
			targetName = "Mercenary Prestige";
		}

		if (isTarget) {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(lowerText);
			boolean isMaxed = false;
			if (m.find()) {
				int current = Integer.parseInt(m.group(1));
				int max = Integer.parseInt(m.group(2));
				if (current >= max) isMaxed = true;
				logInfo(routineLogAllianceAutojoinLine("Found target " + targetName + " with progress " + current + "/" + max));
			}

			boolean shouldBeChecked;
			if (skipMaxedEnabled && isMaxed) {
				shouldBeChecked = false;
				logInfo(routineLogAllianceAutojoinLine("Target " + targetName + " is maxed out and skip maxed is enabled. Will disable."));
			} else {
				shouldBeChecked = isConfiguredEnabled;
			}
			
			int CHECKBOX_X_CENTER = 90;
			int centerY = startY + (135 / 2);
			PointData checkboxCenter = new PointData(CHECKBOX_X_CENTER, centerY);
			AreaData checkboxArea = new AreaData(new PointData(checkboxCenter.getX() - 15, checkboxCenter.getY() - 15),
					new PointData(checkboxCenter.getX() + 15, checkboxCenter.getY() + 15));

			Boolean isCurrentlyChecked = null;
			try {
				RawImageData frame = emuManager.captureScreen(EMULATOR_NUMBER);
				BufferedImage img = dev.frostguard.vision.convert.ImageConverter.toBufferedImage(frame);
				Color golden = new Color(0xFF, 0xC3, 0x33);
				int goldenCount = PixelStats.count(img, checkboxArea, PixelStats.near(golden, 40));
				isCurrentlyChecked = goldenCount > 10;
			} catch (Exception e) {
				logWarning(routineLogAllianceAutojoinLine("Color check failed for checkbox at " + checkboxCenter + ": " + e.getMessage()));
			}
			if (isCurrentlyChecked == null) {
				logWarning(routineLogAllianceAutojoinLine(
						"Checkbox state is unknown for " + targetName + "; leaving it unchanged."));
				return true;
			}

			if (isCurrentlyChecked != shouldBeChecked) {
				logInfo(routineLogAllianceAutojoinLine("Toggling target " + targetName + " from " + isCurrentlyChecked + " to " + shouldBeChecked));
				tapNear(checkboxCenter);
				sleepTask(500);
			} else {
				logInfo(routineLogAllianceAutojoinLine("Target " + targetName + " is already " + (shouldBeChecked ? "enabled" : "disabled") + ", no action needed."));
			}

			return true;
		}
		return false;
	}

private boolean openUpAllianceWarMenu() {
		logDebug(routineLogAllianceAutojoinLine("Entering Alliance War menu"));

		boolean success = navigationHelper.navigateToAllianceMenu(NavigationHelper.AllianceMenu.WAR);

		if (success) {
			sleepTask(1000);

			logDebug(routineLogAllianceAutojoinLine("Alliance War menu opened successfully"));
		} else {
			logError(routineLogAllianceAutojoinLine("Could not navigate to Alliance War menu"));
		}

		return success;
	}

private void hydrateConfiguration() {
		useAllTroops = profile.getConfig(
				ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_USE_ALL_TROOPS_BOOL,
				Boolean.class);

		int rawQueueCount = profile.getConfig(
				ConfigurationKeyEnum.ALLIANCE_AUTOJOIN_QUEUES_INT,
				Integer.class);


		if (rawQueueCount < MIN_QUEUE_COUNT_FLOOR || rawQueueCount > MAX_QUEUE_COUNT_LIMIT) {
			logWarning(routineLogAllianceAutojoinLine("Invalid queue count configured: " + rawQueueCount +
					". Using default: " + DEFAULT_QUEUE_COUNT_VALUE));
			queueCount = DEFAULT_QUEUE_COUNT_VALUE;
		} else {
			queueCount = rawQueueCount;
		}

		logInfo(routineLogAllianceAutojoinLine("Configuration loaded - Use all troops: " + useAllTroops +
				", Queue count: " + queueCount));
	}

private void enableAutoJoinFlow() {
		logInfo(routineLogAllianceAutojoinLine("Enabling auto-join"));
		tapInside(ENABLE_AUTOJOIN_BUTTON_VALUE.topLeft(), ENABLE_AUTOJOIN_BUTTON_VALUE.bottomRight());
		sleepTask(500);

		logDebug(routineLogAllianceAutojoinLine("Auto-join activation command sent"));
	}

private void manageTaskFailure(String reason) {
		logWarning(routineLogAllianceAutojoinLine("Routine pass did not complete: " + reason));

		LocalDateTime retryTime = LocalDateTime.now().plusMinutes(5);
		reschedule(retryTime);

		logInfo(routineLogAllianceAutojoinLine("Task rescheduled to retry in 5 minutes"));
	}

	private void queueNextRun() {
		LocalDateTime nextExecutionTime = LocalDateTime.now()
				.plusHours(SCHEDULE_HOURS_VALUE)
				.plusMinutes(SCHEDULE_MINUTES_VALUE);

		reschedule(nextExecutionTime);

		logInfo(routineLogAllianceAutojoinLine("Alliance auto-join configured successfully. Next execution in "
				+ SCHEDULE_HOURS_VALUE + "h " + SCHEDULE_MINUTES_VALUE + "m"));
	}
}
