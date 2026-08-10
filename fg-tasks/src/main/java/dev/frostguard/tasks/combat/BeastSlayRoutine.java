package dev.frostguard.tasks.combat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.configs.TpDailyTaskEnum;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.api.domain.AccountDescriptor;
import dev.frostguard.data.entity.DailyTask;
import dev.frostguard.data.repository.DailyTaskRepository;
import dev.frostguard.engine.schedule.DelayedTask;
import dev.frostguard.engine.schedule.LaunchPoint;
import dev.frostguard.engine.helper.TemplateSearchHelper.SearchConfig;
import dev.frostguard.engine.nav.SearchConfigConstants;
import dev.frostguard.engine.service.StatisticsService;
import dev.frostguard.engine.service.TaskManagementService;

public class BeastSlayRoutine extends DelayedTask {

	private static final int DEFAULT_STAMINA_RESERVE = 130;
	private static final int STAMINA_COST_PER_ATTACK = 10;

	private final DailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();
	private final TaskManagementService taskManagementService = TaskManagementService.shared();

	private int maxQueues;
	private int beastLevel;
	private int staminaReserve;
	private int maxAttacksLimit;
	private boolean useStaminaItems;
	private int staminaItemReserve;

	/** Tracks the earliest time this task should resume (like GatherRoutine pattern). */
	private LocalDateTime earliestReschedule;

	public BeastSlayRoutine(AccountDescriptor profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected boolean consumesStamina() {
		return true;
	}

	@Override
	protected void execute() {
		earliestReschedule = null;

		// Load configuration
		Integer configMarches = profile.getConfig(ConfigurationKeyEnum.BEAST_HUNTING_MARCHES_INT, Integer.class);
		this.maxQueues = (configMarches != null) ? configMarches : 3;
		Integer configLevel = profile.getConfig(ConfigurationKeyEnum.BEAST_HUNTING_LEVEL_INT, Integer.class);
		this.beastLevel = (configLevel != null) ? configLevel : 30;
		Integer configReserve = profile.getConfig(ConfigurationKeyEnum.STAMINA_RESERVE_INT, Integer.class);
		this.staminaReserve = (configReserve != null) ? configReserve : DEFAULT_STAMINA_RESERVE;

		Integer configMaxAttacks = profile.getConfig(ConfigurationKeyEnum.BEAST_HUNTING_MAX_ATTACKS_INT, Integer.class);
		this.maxAttacksLimit = (configMaxAttacks != null) ? Math.max(0, configMaxAttacks) : 0;

		Boolean configUseItems = profile.getConfig(ConfigurationKeyEnum.BEAST_HUNTING_USE_STAMINA_ITEMS_BOOL, Boolean.class);
		this.useStaminaItems = (configUseItems != null && configUseItems);

		Integer configItemReserve = profile.getConfig(ConfigurationKeyEnum.BEAST_HUNTING_STAMINA_ITEM_RESERVE_INT, Integer.class);
		this.staminaItemReserve = (configItemReserve != null) ? Math.max(0, configItemReserve) : 0;



		long totalSent = StatisticsService.obtain().loadMetrics(profile).getCustomCounters().getOrDefault("Beast Attacks Sent", 0);
		if (maxAttacksLimit > 0 && totalSent >= maxAttacksLimit) {
			logInfo("Reached configured max beast hunt attacks limit (" + maxAttacksLimit + "). Current sent: " + totalSent + ". Task completed.");
			reschedule(LocalDateTime.now().plusHours(12));
			return;
		}

		// Only spend stamina above the reserve, so at least `staminaReserve` stays for Intel/Rally.
		int minToAct = staminaReserve + STAMINA_COST_PER_ATTACK;

		// Use staminaHelper to check stamina (already read during initialization/validation)
		if (!staminaHelper.checkStaminaAndMarchesOrReschedule(minToAct, minToAct, this::reschedule)) {
			if (useStaminaItems) {
				logInfo("Stamina low. Attempting to top-up stamina using items (Reserve items: " + staminaItemReserve + ")...");
				staminaHelper.topUpFromProfile(minToAct + 30, staminaItemReserve);
			}
			if (!staminaHelper.checkStaminaAndMarchesOrReschedule(minToAct, minToAct, this::reschedule)) {
				logInfo("Stamina below reserve (" + staminaReserve + "). Stopping beast hunting to preserve stamina.");
				return;
			}
		}

		int currentStamina = staminaHelper.getCurrentStamina();
		logInfo("Initiating beast attacks. Stamina: " + currentStamina + ", Reserve: " + staminaReserve
				+ ", Max queues: " + maxQueues + ", Beast level: " + beastLevel
				+ ", Max attacks limit: " + (maxAttacksLimit > 0 ? maxAttacksLimit : "Unlimited")
				+ ", Use stamina items: " + useStaminaItems);

		int attacksDone = 0;

		// Fill available queues with beast attacks, never dipping below the reserve or exceeding maxAttacksLimit
		while (currentStamina - staminaReserve >= STAMINA_COST_PER_ATTACK && attacksDone < maxQueues) {

			totalSent = StatisticsService.obtain().loadMetrics(profile).getCustomCounters().getOrDefault("Beast Attacks Sent", 0);
			if (maxAttacksLimit > 0 && totalSent >= maxAttacksLimit) {
				logInfo("Reached max beast attacks limit (" + maxAttacksLimit + "). Ending hunting loop.");
				break;
			}

			// If current stamina is low but items are enabled, top up
			if (currentStamina - staminaReserve < STAMINA_COST_PER_ATTACK && useStaminaItems) {
				logInfo("Stamina low during hunt loop. Using stamina item...");
				staminaHelper.topUpFromProfile(minToAct + 30, staminaItemReserve);
				currentStamina = staminaHelper.getCurrentStamina();
			}

			if (currentStamina - staminaReserve < STAMINA_COST_PER_ATTACK) {
				logInfo("Stamina reached minimum reserve (" + staminaReserve + "). Stopping hunt loop.");
				break;
			}

			sleepTask(6000);
			// Open the creature search menu
			tapRandomPoint(new PointData(25, 850), new PointData(67, 898));
			sleepTask(1500);

			// Select the "Beasts" tab by template, swiping the tab row until found.
			if (!selectBeastTab()) {
				logWarning("Could not locate the Beasts tab (an event may have changed the search menu). "
						+ "Skipping attack to avoid hitting the wrong creature. Retrying in 5 min.");
				updateReschedule(LocalDateTime.now().plusMinutes(5));
				break;
			}

			// Select beast level (cached per profile so it only adjusts once per profile/level change)
			selectBeastLevel(beastLevel);
			sleepTask(500);
			// click search
			tapRandomPoint(new PointData(301, 1200), new PointData(412, 1229));
			sleepTask(6000);

			// click attack - search for the attack button template
			tapRandomPoint(new PointData(270, 600), new PointData(460, 630));
			sleepTask(6000);
			
			ImageSearchResultData attackBtn = templateSearchHelper.locatePattern(
					TemplatesEnum.GAME_HOME_SHORTCUTS_ATTACK, SearchConfig.builder().build());
			if (attackBtn != null && attackBtn.isFound()) {
				tapPoint(attackBtn.getPoint());
			}
			
			sleepTask(3000);

			try {
				ConfigurationKeyEnum flagKey;
				switch (attacksDone) {
					case 0: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_1_FLAG_STRING; break;
					case 1: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_2_FLAG_STRING; break;
					case 2: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_3_FLAG_STRING; break;
					case 3: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_4_FLAG_STRING; break;
					case 4: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_5_FLAG_STRING; break;
					case 5: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_6_FLAG_STRING; break;
					default: flagKey = ConfigurationKeyEnum.BEAST_HUNTING_MARCH_1_FLAG_STRING; break;
				}

				String currentFlagString = profile.getConfig(flagKey, String.class);
				if (currentFlagString != null && !currentFlagString.trim().isEmpty()
						&& !currentFlagString.trim().equalsIgnoreCase("No Flag")) {
					try {
						int currentFlagNumber = Integer.parseInt(currentFlagString.trim());
						logInfo("Selecting formation preset #" + currentFlagNumber + " for beast march " + (attacksDone + 1));
						marchHelper.selectFlag(currentFlagNumber);
						sleepTask(300);
					} catch (NumberFormatException e) {
						logWarning("Invalid flag number in config for beast march " + (attacksDone + 1) + ": " + currentFlagString);
					}
				}

				// Use staminaHelper to parse travel time via OCR (uses CommonGameAreas.TRAVEL_TIME_OCR_AREA)
				long travelSeconds = staminaHelper.parseTravelTime();

				// confirm attack
				tapRandomPoint(new PointData(450, 1183), new PointData(640, 1240));

				// Update stamina tracking
				staminaHelper.subtractStamina(STAMINA_COST_PER_ATTACK, false);
				currentStamina = staminaHelper.getCurrentStamina();
				attacksDone++;
				StatisticsService.obtain().addToCounter(profile, "Beast Attacks Sent", 1);

				// March returns in ~2x travel time
				long returnSeconds = (travelSeconds > 0) ? travelSeconds * 2 : 120;
				LocalDateTime marchReturn = LocalDateTime.now().plusSeconds(returnSeconds);
				updateReschedule(marchReturn);

				logInfo("Beast attacked. March returns in ~" + returnSeconds
						+ "s. Remaining stamina: " + currentStamina + ", attacks done this loop: " + attacksDone
						+ ", total attacks: " + (totalSent + 1));

			} catch (Exception e) {
				logError("Failed during beast attack: " + e.getMessage());
				// Conservative fallback reschedule
				updateReschedule(LocalDateTime.now().plusMinutes(5));
				break;
			}
		}

		// Finalize: reschedule to earliest beast return time (freeing the thread for other tasks)
		finalizeReschedule();
	}

	@Override
	protected LaunchPoint getRequiredStartLocation() {
		return LaunchPoint.WORLD;
	}

	/**
	 * Selects the "Beasts" tab in the creature search menu by locating its icon
	 * and swiping the tab row until it is found. Mirrors the robust approach of
	 * {@link PolarTerrorHuntingRoutine#openUpPolarsMenu}, so an inserted event tab
	 * (e.g. "Berserk Cryptid" during a Cryptid event) can no longer shift a blind
	 * tap onto the wrong creature.
	 *
	 * @return {@code true} once the Beasts tab has been tapped; {@code false} if the
	 *         icon could not be located after several swipes.
	 */
	private boolean selectBeastTab() {
		ImageSearchResultData beastTab = templateSearchHelper.locatePattern(
				TemplatesEnum.BEAST_SEARCH_ICON, SearchConfigConstants.SINGLE_WITH_RETRIES);
		for (int i = 0; i < 4 && (beastTab == null || !beastTab.isFound()); i++) {
			swipe(new PointData(40, 913), new PointData(678, 913));
			sleepTask(500);
			beastTab = templateSearchHelper.locatePattern(
					TemplatesEnum.BEAST_SEARCH_ICON, SearchConfigConstants.SINGLE_WITH_RETRIES);
		}
		if (beastTab == null || !beastTab.isFound()) {
			return false;
		}
		tapPoint(beastTab.getPoint());
		sleepTask(1000);
		return true;
	}

	/**
	 * Mirrors PolarTerrorHuntingRoutine: if Intel is enabled and scheduled to run
	 * within the next 5 minutes, Beast Hunting defers so it doesn't consume stamina
	 * Intel is about to need.
	 */
	private boolean shouldDeferToIntel() {
		if (!Boolean.TRUE.equals(profile.getConfig(ConfigurationKeyEnum.INTEL_BOOL, Boolean.class))) {
			return false;
		}
		if (!taskManagementService.lookupTaskState(profile.getId(), TpDailyTaskEnum.INTEL.getId()).isScheduled()) {
			return false;
		}
		DailyTask intel = iDailyTaskRepository.findByAccountIdAndTaskType(profile.getId(), TpDailyTaskEnum.INTEL);
		return intel != null
				&& ChronoUnit.MINUTES.between(LocalDateTime.now(), intel.getScheduledAt()) < 5;
	}

	// ========================================================================
	// SCHEDULING HELPERS (GatherRoutine pattern)
	// ========================================================================

	private void updateReschedule(LocalDateTime t) {
		if (earliestReschedule == null || t.isBefore(earliestReschedule)) {
			earliestReschedule = t;
		}
	}

	private void finalizeReschedule() {
		if (earliestReschedule != null) {
			logInfo("Beast Hunting finished. Rescheduling to " + earliestReschedule + " (earliest march return).");
			reschedule(earliestReschedule);
		} else {
			logInfo("Beast Hunting finished. No marches dispatched. Rescheduling in 5 minutes.");
			reschedule(LocalDateTime.now().plusMinutes(5));
		}
	}
	// ========================================================================
	// LEVEL SELECTION CACHING
	// ========================================================================

	private static final Map<Long, Integer> profileBeastLevelCache = new ConcurrentHashMap<>();

	private void selectBeastLevel(int targetLevel) {
		Integer cached = profileBeastLevelCache.get(profile.getId());
		if (cached != null && cached.equals(targetLevel)) {
			logInfo("Beast level " + targetLevel + " already cached for profile " + profile.getName() + ". Skipping level reset.");
			return;
		}

		logInfo("First time/level change: setting beast level to " + targetLevel + " for profile " + profile.getName());
		// go to level 1
		swipe(new PointData(180, 1050), new PointData(1, 1050));
		sleepTask(300);

		// select beast level
		if (targetLevel > 1) {
			tapRandomPoint(new PointData(470, 1040), new PointData(500, 1070), targetLevel - 1, 80);
			sleepTask(500);
		}

		profileBeastLevelCache.put(profile.getId(), targetLevel);
	}

}
