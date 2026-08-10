package dev.frostguard.tasks.alliance;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.nav.SearchConfigConstants;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.vision.convert.GameTimeUtils;
import java.time.LocalDateTime;

public class AllianceChestRoutine extends DelayedTask {

private static final int TAB_CHANGE_WAIT_TIME_MS = 500;

private static final int CLAIM_WAIT_TIME_MS = 1500;

private static final int SHORT_WAIT_TIME_MS = 300;

public AllianceChestRoutine(AccountDescriptor profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected boolean acceptsInjections() {
		return false;
	}

	@Override
	protected void execute() {
		logInfo(routineLogAllianceChestLine("Starting Alliance Chest routine."));
		reachAllianceScreen();

		if (!openUpAllianceChestScreen()) {
			logWarning(routineLogAllianceChestLine("Opening alliance chest screen via fallback tap at (540, 680)."));
			tapRandomPoint(new PointData(480, 640), new PointData(600, 720));
			sleepTask(2000);
		}

		gatherLootChests();
		gatherAllianceGifts();
		gatherHonorChest();

		restoreHomeScreen();
		queueNextRun();
	}

	private String routineLogAllianceChestLine(String note) {
		return "AllianceChestRoutine | " + note;
	}

	private boolean openUpAllianceChestScreen() {
		ImageSearchResultData allianceChestResult = templateSearchHelper.locatePattern(
				TemplatesEnum.ALLIANCE_CHEST_BUTTON,
				dev.frostguard.engine.helper.TemplateSearchHelper.SearchConfig.builder()
						.withThreshold(60)
						.withMaxAttempts(2)
						.build());
		if (allianceChestResult != null && allianceChestResult.isFound()) {
			tapPoint(allianceChestResult.getPoint());
			sleepTask(1500);
			return true;
		}
		return false;
	}

private void gatherIndividualGifts() {
		int giftsClaimed = 0;
		int consecutiveFailures = 0;
		int maxConsecutiveFailures = 3;


		while (consecutiveFailures < maxConsecutiveFailures) {
			ImageSearchResultData claimButton = templateSearchHelper.locatePattern(
					TemplatesEnum.ALLIANCE_CHEST_CLAIM_BUTTON,
					SearchConfigConstants.DEFAULT_SINGLE);

			if (claimButton.isFound()) {
				logDebug(routineLogAllianceChestLine("Collecting individual gift #" + (giftsClaimed + 1)));
				tapPoint(claimButton.getPoint());
				sleepTask(CLAIM_WAIT_TIME_MS);
				giftsClaimed++;
				consecutiveFailures = 0;


				dismissPopupIfPresent();
			} else {
				consecutiveFailures++;


				if (consecutiveFailures >= maxConsecutiveFailures) {
					logDebug(routineLogAllianceChestLine("Zero additional individual gifts detected."));
					break;
				}
			}
		}

		if (giftsClaimed > 0) {
			logInfo(routineLogAllianceChestLine("Successfully collected " + giftsClaimed + " individual gifts."));
		} else {
			logInfo(routineLogAllianceChestLine("Zero individual gifts to claim."));
		}
	}

	private void dismissPopupIfPresent() {
		tapPoint(new PointData(430, 900));
		sleepTask(SHORT_WAIT_TIME_MS);
	}

private boolean reachAllianceScreen() {
		logInfo(routineLogAllianceChestLine("Moving to alliance screen"));
		tapRandomPoint(new PointData(493, 1187), new PointData(561, 1240));
		sleepTask(2500);
		return true;
	}

	private void gatherAllianceGifts() {
		logInfo(routineLogAllianceChestLine("Entering alliance gifts section (Gifts tab)."));
		tapRandomPoint(new PointData(410, 375), new PointData(626, 420));
		sleepTask(1000);

		ImageSearchResultData claimAllButton = templateSearchHelper.locatePattern(
				TemplatesEnum.ALLIANCE_CHEST_CLAIM_ALL_BUTTON,
				dev.frostguard.engine.helper.TemplateSearchHelper.SearchConfig.builder()
						.withThreshold(60)
						.withMaxAttempts(2)
						.build());

		if (claimAllButton != null && claimAllButton.isFound()) {
			logInfo(routineLogAllianceChestLine("'Claim All' button detected for gifts at " + claimAllButton.getPoint() + ". Collecting all gifts."));
			tapPoint(claimAllButton.getPoint());
			sleepTask(CLAIM_WAIT_TIME_MS);
			dismissPopupIfPresent();
		} else {
			logInfo(routineLogAllianceChestLine("Template 'Claim All' not matched. Tapping (540, 1204) & (360, 1204) for gifts 'Claim All'."));
			tapPoint(new PointData(540, 1204));
			sleepTask(CLAIM_WAIT_TIME_MS);
			dismissPopupIfPresent();
			tapPoint(new PointData(360, 1204));
			sleepTask(CLAIM_WAIT_TIME_MS);
			dismissPopupIfPresent();
			gatherIndividualGifts();
		}
		sleepTask(SHORT_WAIT_TIME_MS);
	}

private void restoreHomeScreen() {
		logInfo(routineLogAllianceChestLine("Returning to home screen."));
		pressBack();
		sleepTask(SHORT_WAIT_TIME_MS);
		pressBack();
		sleepTask(SHORT_WAIT_TIME_MS);


	}

private void gatherLootChests() {
		logInfo(routineLogAllianceChestLine("Collecting loot chests."));
		tapRandomPoint(new PointData(56, 375), new PointData(320, 420));
		sleepTask(TAB_CHANGE_WAIT_TIME_MS);


		tapPoint(new PointData(360, 1204));
		sleepTask(CLAIM_WAIT_TIME_MS);


		dismissPopupIfPresent();
		sleepTask(SHORT_WAIT_TIME_MS);
	}

private void gatherHonorChest() {
		boolean honorChestEnabled = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_HONOR_CHEST_BOOL, Boolean.class);

		if (honorChestEnabled) {
			logInfo(routineLogAllianceChestLine("Collecting honor chest."));
			tapRandomPoint(new PointData(320, 200), new PointData(400, 250));
			sleepTask(TAB_CHANGE_WAIT_TIME_MS);


			dismissPopupIfPresent();
		} else {
			logInfo(routineLogAllianceChestLine("Honor chest collection is disabled. Skipping."));
		}
	}

private void queueNextRun() {
		int offsetMinutes = profile.getConfig(ConfigurationKeyEnum.ALLIANCE_CHESTS_OFFSET_INT, Integer.class);
		LocalDateTime nextExecutionTime = LocalDateTime.now().plusMinutes(offsetMinutes);
		nextExecutionTime = nextExecutionTime.isAfter(GameTimeUtils.dailyResetTime()) ? GameTimeUtils.dailyResetTime()
				: nextExecutionTime;
		reschedule(nextExecutionTime);
		logInfo(routineLogAllianceChestLine("Alliance chest task completed. Next run at: " + nextExecutionTime.format(DATETIME_FORMATTER)));
	}

private void deferAndExit(String reason) {
		logWarning(routineLogAllianceChestLine(reason + ". Planning next run task to run in 5 minutes."));
		LocalDateTime nextExecutionTime = LocalDateTime.now().plusMinutes(5);
		reschedule(nextExecutionTime);
	}
}
